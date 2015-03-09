package org.serviceHub.actors

import akka.actor.Actor
import org.serviceHub.actors.ProcessorActor.DeliverMessage
import org.serviceHub.domain.{Service, Message}
import spray.client.pipelining._

import scala.util.{Failure, Success}

object ProcessorActor {
  case class DeliverMessage(msg: Message, service: Service)
}

class ProcessorActor extends Actor {
  import context.{system => _, _}
  import org.serviceHub.domain.MessageJsonProtocol._

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
      svc.enqueueInput(attemptedMsg)
    else
      svc.kill(attemptedMsg)
  }

  override def receive: Receive = {
    case DeliverMessage(msg, svc) => deliver(msg, svc)
  }
}
