package org.serviceHub.actors

import akka.actor.Actor
import org.serviceHub.actors.QueueMessages.{Enqueue, Enqueued}
import org.serviceHub.domain.{Message, ServicesRepository}

class OutgoingActor(repo: ServicesRepository) extends Actor {
  override def receive: Receive = {
    case Enqueue(msg) =>
      val _sender = sender()
      enqueue(msg)
      _sender ! Enqueued()
  }

  def enqueue(msg: Message): Unit = {
    val publisher = repo.getPublisherOf(msg).get
    publisher.enqueueOutgoing(msg)
  }
}
