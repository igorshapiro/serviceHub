package org.serviceHub.actors

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import org.serviceHub.actors.ServiceActor.{Handle, Initialize, Initialized, Send}
import org.serviceHub.domain.{Message, Service, ServicesRepository}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ServiceActor {
  case class Initialize()
  case class Initialized()
  case class Handle(msg: Message)
  case class Send(msg: Message)
}

class ServiceActor(service: Service, repository: ServicesRepository,
                  _outgoingQueueActor: ActorRef = null
                    ) extends Actor {
  import org.serviceHub.domain.MessageJsonProtocol._
  import spray.client.pipelining._


  implicit val timeout = Timeout(120 seconds)
  import context.{system => _, _}

  def initializeActors = {
    Future.sequence(Seq[Future[Any]](
    ))
  }

  override def receive: Receive = {
    case Initialize() =>
      val _sender = sender()
      initializeActors onComplete {
        case Success(_) =>
          _sender ! Initialized()
          become(running)
        case Failure(e) =>
          println(s"Unable to initialize service: ${service.name} - $e:\n")
          e.printStackTrace()
      }

  }

  def running: Receive = {
    case Send(msg) =>
      println(s"Sending $msg")
      service.enqueueOutgoing(msg)
    case Handle(msg) =>
      handle(msg);
  }

  def handle(msg: Message) = {
    if (msg.reachedMaxAttempts) kill(msg)
    val url = service.getEndpointUrlFor(msg)
    doHttpPost(url, msg)
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