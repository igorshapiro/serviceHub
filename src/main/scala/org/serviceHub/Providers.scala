package org.serviceHub

import org.serviceHub.domain.Service
import org.serviceHub.providers.queue.{MQProvider, RabbitMQProvider}
import spray.http.Uri

object Providers {
  def createMQProvider(service: Service): MQProvider = {
    Uri(service.queue) match {
      case Uri("rabbitmq", authority, _, _, _) => new RabbitMQProvider(service)
    }
  }
}
