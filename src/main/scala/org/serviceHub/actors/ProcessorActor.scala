package org.serviceHub.actors

import akka.actor.{ActorRef, Actor}
import akka.util.Timeout
import org.serviceHub.actors.ProcessorActor.DeliverMessage
import org.serviceHub.actors.queue.MQActor.{InputQueue, Enqueue}
import org.serviceHub.actors.storage.StorageActor.{Stored, DeadStorage, Store}
import org.serviceHub.domain.{Service, Message}
import spray.client.pipelining._
import akka.pattern.ask
import scala.concurrent.duration._

import scala.language.postfixOps
import scala.util.{Failure, Success}

object ProcessorActor {
  case class DeliverMessage(msg: Message, service: Service)
}

class ProcessorActor(mqActor: ActorRef, storageActor: ActorRef) extends Actor {
  import context.{system => _, _}
  import org.serviceHub.domain.MessageJsonProtocol._

  implicit val timeout = new Timeout(5 seconds)

  def deliver(msg: Message, service: Service): Unit = {
    val url = service.getEndpointUrlFor(msg)
    val pipeline = addHeader("Connection", "close") ~> sendReceive

    val response = pipeline { Post(url, msg) }
    response onComplete {
      case Success(r) => r.status.intValue / 100 match {
        case 4 | 5 => killOrReenqueue(service, msg)
        case 3 => schedule(msg)
        case 2 => handleSuccess(msg)
      }
      case Failure(e) => killOrReenqueue(service, msg)
    }
  }

  def handleSuccess(msg: Message) {}
  def schedule(msg: Message) = ???
  def killOrReenqueue(svc: Service, msg: Message) = {
    val attemptedMsg = msg.attempted
    if (attemptedMsg.maxAttempts > attemptedMsg.attemptsMade)
      mqActor ! Enqueue(svc, InputQueue, attemptedMsg)
    else
      (storageActor ? Store(msg.attempted, svc, DeadStorage)).mapTo[Stored]
  }

  override def receive: Receive = {
    case DeliverMessage(msg, svc) =>
      deliver(msg, svc)
  }
}
