package org.serviceHub.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import org.serviceHub.actors.queue.MQActor._
import org.serviceHub.domain.{Message, ServicesRepository}

import scala.concurrent.duration._

class DispatchingActor(repository: ServicesRepository, queueActor: ActorRef) extends Actor {
  implicit val timeout = Timeout(5 seconds)
  import context._

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    repository.services.foreach(queueActor ! Consume(_, OutgoingQueue, self))
  }

  override def receive: Receive = {
    case MessageArrived(msg, _, ack) =>
      dispatch(msg)
      ack()
  }

  def dispatch(message: Message): Unit = {
    repository.getSubscribersFor(message).foreach(sub => {
      val enqueued = (queueActor ? Enqueue(sub, InputQueue, message)).mapTo[MessageEnqueued]
      enqueued onSuccess {
        case _ =>
      }
    })
  }

}
