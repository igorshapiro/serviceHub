package org.serviceHub.actors

import org.serviceHub.domain.{Message, Service}

object QueueMessages {
  case class Enqueue(msg: Message)
  case class Enqueued()
  case class Initialize(service: Service)
  case class Initialized()
}
