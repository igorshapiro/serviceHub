package org.serviceHub.providers.queue

import com.rabbitmq.client
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client._
import org.serviceHub.domain.{Message, Service}
import org.serviceHub.providers.queue.MQProvider.MessageHandler
import spray.http.Uri
import spray.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object RabbitMQChannelPool {
  class EndpointPool(endpoint: String) {
    val max_channels_per_connection = 5
    val max_acquires_per_channel = 5
    val factory = new ConnectionFactory()
    val consumerChannels = ArrayBuffer[(Connection, Channel)]()
    val connections = mutable.Map[Connection, mutable.Map[Channel, Int]]().withDefaultValue(mutable.Map[Channel, Int]())

    implicit def endpointAddress: Address = {
      val uri: Uri = endpoint
      val port = if (uri.effectivePort == -1) 5672 else uri.effectivePort
      println(s"Will connect to ${uri.authority.host.address}:$port")
      new Address(uri.authority.host.address, port)
    }

    def newConnection = factory.newConnection(List(endpointAddress).toArray)

    def getConsumerChannel: Channel = {
      val con = newConnection
      val channel = con.createChannel()
      consumerChannels.append((con, channel))
      channel.getConnection
      channel
    }

    def getNonConsumerChannel: Channel = {
      // Try to find a channel with acquires < max_acquires_per_channel
      connections.flatMap(c => c._2).find(_._2 < max_acquires_per_channel) match {
        case Some((channel, _)) =>
          return channel
        case None =>
      }
      connections.find(_._2.size < max_channels_per_connection) match {
        case Some((con, channels)) =>
          val channel = con.createChannel()
          channels.put(channel, 1)
          channel
        case None =>
          val con = newConnection
          val channel = con.createChannel
          connections.put(con, mutable.Map((channel, 1)))
          channel
      }
    }

    def release(channel: Channel) = {
      val con = channel.getConnection()
      connections(con)(channel) -= 1
    }

    def executeInChannel(block: Channel => Unit): Unit = {
      val channel = getNonConsumerChannel
      try {
        block(channel)
      }
      finally {
        release(channel)
      }
    }

    def stop() = {
      consumerChannels.foreach { x =>
        x._2.close()    // Close the channel
        x._1.close()    // Then close the connection
      }
      consumerChannels.clear()

      connections.foreach { c =>
        c._2.foreach(_._1.close())    // Close all channels
        c._1.close()                  // Close all connections
      }
      connections.clear()
    }
  }

  val pools = mutable.Map[String, EndpointPool]().withDefault(new EndpointPool(_))

  def apply(service: Service) = pools(service.queue)
  def stop(): Unit = pools.foreach(_._2.stop())
}

class RabbitMQProvider(service: Service) extends MQProvider(service) {
  import org.serviceHub.domain.MessageJsonProtocol._
  val factory = new ConnectionFactory()

  def initQueue(channel: client.Channel, queue: String) = channel.queueDeclare(queue, true, false, false, null)

  lazy val initialize: () => Unit = {
    RabbitMQChannelPool(service).executeInChannel { c =>
      initQueue(c, inputQueue)
      initQueue(c, outgoingQueue)
      initQueue(c, deadQueue)
    }
    () => Unit
  }

  override def send(queue: String, msg: Message): Unit = {
    initialize()

    import org.serviceHub.domain.MessageJsonProtocol._
    val bytes = msg.toJson.prettyPrint.getBytes("UTF-8")

    // http://stackoverflow.com/questions/6386117/rabbitmq-use-of-immediate-and-mandatory-bits
    RabbitMQChannelPool(service).executeInChannel { ch =>
      ch.basicPublish("", queue, false, false, null, bytes)
    }
  }

  override def consume(queue: String, handler: MessageHandler): ConsumerControl = {
    initialize()

    val channel = RabbitMQChannelPool(service).getConsumerChannel
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(
                                   consumerTag: String,
                                   envelope: Envelope,
                                   properties: BasicProperties,
                                   body: Array[Byte]): Unit =  {
        val msg = new String(body, "UTF-8").parseJson.convertTo[Message]
        handler(msg)
        channel.basicAck(envelope.getDeliveryTag, false)
      }
    }
    channel.basicConsume(queue, false, consumer)
    new ConsumerControl(channel)
  }


  override protected def purge(queue: String): Unit= RabbitMQChannelPool(service).executeInChannel { ch =>
    ch.queuePurge(queue)
  }

  private def getConnection = factory.newConnection()
}
