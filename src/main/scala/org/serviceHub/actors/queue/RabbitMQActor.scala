package org.serviceHub.actors.queue

import akka.actor.{ActorRef, FSM}
import com.rabbitmq.client._
import org.serviceHub.actors.queue.MQActor._
import org.serviceHub.actors.queue.RabbitMQActor.{Active, RabbitMQServices, State}
import org.serviceHub.domain.{Message, Service}
import spray.http.Uri

class RabbitMQActor extends FSM[State, RabbitMQServices] {
  startWith(Active, new RabbitMQServices())

  when (Active) {
    case Event(Consume(svc, queueType, target), data: RabbitMQServices) =>
      val newData = data.withService(svc)
      newData(svc).subscribe(queueType, target)
      stay using newData
    case Event(Enqueue(svc, queueType, msg), data: RabbitMQServices) =>
      val newData = data.withService(svc)
      newData(svc).enqueue(queueType, msg)
      sender() ! MessageEnqueued(msg)
      stay using newData
    case Event(StopAll, data: RabbitMQServices) =>
      val stopped = data.stopped()
      sender() ! StoppedAll
      stay using stopped
  }

  initialize()
}

object RabbitMQActor {
  def createConnectionFactory(service: Service): ConnectionFactory = {
    val url = Uri(service.queue)
    val factory = new ConnectionFactory()
    factory.setHost(url.authority.host.address)
    factory.setPort(if (url.authority.port == 0) 5672 else url.authority.port)
    factory.setVirtualHost(url.path.toString())
    factory
  }

  sealed trait State
  case object Active extends State

  sealed trait Data
  case class RabbitMQServices(services: Map[Service, RabbitMQService] = Map[Service, RabbitMQService]()) extends Data {
    def apply(service: Service) = services(service)

    def withService(service: Service) = {
      if (!services.contains(service)) {
        val rabbitMQService = RabbitMQService(service)
        val mapWithService = services + ((service, rabbitMQService))
        this.copy(services = mapWithService)
      }
      else this
    }

    def stopped(): RabbitMQServices = {
      services.foreach(_._2.stop())
      RabbitMQServices()
    }
  }

  case class RabbitMQService(service: Service) {
    import spray.json._
    import org.serviceHub.domain.MessageJsonProtocol._

    val factory = RabbitMQActor.createConnectionFactory(service)
    val connection = factory.newConnection()
    val channel = connection.createChannel()
    println("Started channel")

    channel.queueDeclare(MQActor.resolveQueueName(service, InputQueue), true, false, false, null)
    channel.queueDeclare(MQActor.resolveQueueName(service, OutgoingQueue), true, false, false, null)

    def subscribe(queueType: QueueType, target: ActorRef) = {
      val queueName = MQActor.resolveQueueName(service, queueType)
      channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
        override def handleDelivery(consumerTag: String,
                                    envelope: Envelope,
                                    properties: AMQP.BasicProperties,
                                    body: Array[Byte]): Unit = {
          val msg = new String(body, "UTF-8").parseJson.convertTo[Message]

          target ! MessageArrived(msg, service)
          channel.basicAck(envelope.getDeliveryTag, false)      // TODO: REMOVE THIS!!! Should be handled by the receiving actor
        }
      })
    }

    def enqueue(queueType: QueueType, msg: Message) = {
      val queueName = MQActor.resolveQueueName(service, queueType)

      import org.serviceHub.domain.MessageJsonProtocol._
      val bytes = msg.toJson.prettyPrint.getBytes("UTF-8")

      channel.basicPublish("", queueName, null, bytes)
    }

    def stop() = {
      channel.close()
      connection.close()
      println("Stopped channel")
    }
  }
}