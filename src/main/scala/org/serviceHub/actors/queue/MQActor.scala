package org.serviceHub.actors.queue

import akka.actor.ActorRef
import org.serviceHub.actors.queue.MQActor.{InputQueue, OutgoingQueue}
import org.serviceHub.domain.{Message, Service}

import scala.language.implicitConversions

object ServiceExtensions {
  class ServiceQueues(svc: Service) {
    def inputQueue = MQActor.resolveQueueName(svc, InputQueue)
    def outgoingQueue = MQActor.resolveQueueName(svc, OutgoingQueue)
  }

  implicit def serviceExtensions(svc: Service): ServiceQueues = new ServiceQueues(svc)
}

object MQActor {
  case class Enqueue(service: Service, queueType: QueueType, msg: Message)
  case class MessageEnqueued(message: Message)
  case class Consume(service: Service, queueType: QueueType, target: ActorRef)
  case class MessageArrived(msg: Message, service: Service)
  case object StopAll
  case object StoppedAll

  sealed trait QueueType
  case object InputQueue extends QueueType
  case object OutgoingQueue extends QueueType

  def resolveQueueName(service: Service, queueType: QueueType) = queueType match {
    case InputQueue => s"${service.name}_in"
    case OutgoingQueue => s"${service.name}_out"
  }

  import org.serviceHub.domain.MessageJsonProtocol._
  import spray.json._

  def asJsonString(msg: Message) = msg.toJson.prettyPrint
  def asJsonUTF8Bytes(msg: Message) = asJsonString(msg).getBytes("UTF-8")
  def fromJsonString(s: String) = s.parseJson.convertTo[Message]
  def fromJsonUTF8Bytes(buf: Array[Byte]) = fromJsonString(new String(buf, "UTF-8"))
}
