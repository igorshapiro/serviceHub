package org.serviceHub.actors

import akka.actor.Actor
import akka.util.Timeout
import org.serviceHub.actors.ServiceActor.{Handle, Initialize, Initialized}
import org.serviceHub.domain.{Message, Service, ServicesRepository}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ServiceActor {
  case class Initialize()
  case class Initialized()
  case class Handle(msg: Message)
}

class ServiceActor(service: Service, repository: ServicesRepository) extends Actor {
  import spray.client.pipelining._
  import org.serviceHub.domain.MessageJsonProtocol._

  implicit val timeout = Timeout(120 seconds)
  import context.{system => _, _}

  override def receive: Receive = {
    case Initialize() =>
      become(running)
      sender ! Initialized()
  }

  def running: Receive = {
    case Handle(msg) => handle(msg);
  }

  def handle(msg: Message) = {
    if (msg.reachedMaxAttempts) kill(msg)
    val url = service.getEndpointUrlFor(msg)
    val serviceResponse = doHttpPost(url, msg)
  }

  def kill(msg: Message) = ???

  def doHttpPost(url: String, msg: Message): Unit = {
    val pipeline = sendReceive

    val response = pipeline {
      Post(url, msg)
    }
    response onComplete {
      case Success(res) =>
        res.status.intValue / 100 match {
          case 4 | 5 => handle(msg.attempted)
          case 2 => println("Message handling succeeded")
          case 3 => throw new NotImplementedError("Scheduling not implemented")
        }
      case Failure(res) => self ! Handle(msg.attempted)
    }
  }
}