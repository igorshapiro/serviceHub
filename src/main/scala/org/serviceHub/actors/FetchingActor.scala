package org.serviceHub.actors

import akka.actor.{Actor, ActorRef}
import org.serviceHub.domain.Service

/**
 * Created by igor on 3/8/15.
 */
class FetchingActor(service: Service, processor: ActorRef) extends Actor {
  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    service.consumeInput(processor ! _)
  }

  override def receive: Receive = {
    case x => println(s"Unexpected message received: $x")
  }
}
