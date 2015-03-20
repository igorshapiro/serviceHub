package org.serviceHub.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import org.serviceHub.actors.QueueMessages.{Enqueue, Enqueued}
import org.serviceHub.actors.queue.MQActor
import org.serviceHub.actors.queue.MQActor.{MessageEnqueued, OutgoingQueue}
import org.serviceHub.domain.ServicesRepository

import scala.concurrent.duration._
import scala.util.Success

class OutgoingActor(repo: ServicesRepository, queueActor: ActorRef) extends Actor {
  implicit val timeout = Timeout(5 seconds)
  import context._

  override def receive: Receive = {
    case Enqueue(msg) =>
      val _sender = sender
      val enqueued = (queueActor ? MQActor.Enqueue(repo.getPublisherOf(msg).get, OutgoingQueue, msg)).mapTo[MessageEnqueued]
      enqueued.onComplete {
        case Success(_) => _sender ! Enqueued()
      }
  }
}
