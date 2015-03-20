package org.serviceHub.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import org.serviceHub.actors.ProcessorActor.DeliverMessage
import org.serviceHub.actors.queue.MQActor.{Consume, InputQueue, MessageArrived}
import org.serviceHub.domain.ServicesRepository

class FetchingActor(repo: ServicesRepository, processor: ActorRef, queueActor: ActorRef) extends Actor with ActorLogging {
  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    repo.services.foreach(queueActor ! Consume(_, InputQueue, self))
  }

  override def receive: Receive = {
    case MessageArrived(msg, svc) =>
      processor ! DeliverMessage(msg, svc)
  }
}
