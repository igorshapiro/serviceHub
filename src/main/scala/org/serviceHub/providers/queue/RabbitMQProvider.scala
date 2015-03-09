package org.serviceHub.providers.queue

import com.rabbitmq.client
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{Channel, ConnectionFactory, DefaultConsumer, Envelope}
import org.serviceHub.domain.{Message, Service}
import org.serviceHub.providers.queue.MQProvider.MessageHandler
import spray.json._

object MQProvider {
  type MessageHandler = Message => Unit
}

class ConsumerControl(channel: Channel) {
  def stop = channel.close()
}

abstract class MQProvider(service: Service) {
  protected def send(queue: String, msg: Message)
  protected def consume(queue: String, handler: MessageHandler): ConsumerControl
  protected def purge(queue: String): Unit = ???

  protected def outgoingQueue = s"${service.name}_out"
  protected def inputQueue = s"${service.name}_in"
  protected def deadQueue = s"${service.name}_dead"
  
  def purgeAllQueues = List(outgoingQueue, inputQueue, deadQueue).foreach(purge)

  def sendOutgoing(msg: Message) = send(outgoingQueue, msg)
  def consumeOutgoing(handler: MessageHandler) = consume(outgoingQueue, handler)

  def sendInput(msg: Message) = send(inputQueue, msg)
  def consumeInput(handler: MessageHandler) = consume(inputQueue, handler)
}

class RabbitMQProvider(service: Service) extends MQProvider(service) {
  import org.serviceHub.domain.MessageJsonProtocol._
  val factory = new ConnectionFactory()

  def initQueue(channel: client.Channel, queue: String) = channel.queueDeclare(queue, true, false, false, null)

  lazy val initialize: () => Unit = {
    val c = getChannel
    initQueue(c, inputQueue)
    initQueue(c, outgoingQueue)
    initQueue(c, deadQueue)
    () => Unit
  }

  override def send(queue: String, msg: Message): Unit = {
    initialize()

    import org.serviceHub.domain.MessageJsonProtocol._
    val bytes = msg.toJson.prettyPrint.getBytes("UTF-8")

    // http://stackoverflow.com/questions/6386117/rabbitmq-use-of-immediate-and-mandatory-bits
    getChannel.basicPublish("", queue, false, false, null, bytes)
  }

  override def consume(queue: String, handler: MessageHandler): ConsumerControl = {
    initialize()

    val channel = getChannel
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


  override protected def purge(queue: String): Unit= getChannel.queuePurge(queue)

  private def getConnection = factory.newConnection()
  private def getChannel = getConnection.createChannel()
}
