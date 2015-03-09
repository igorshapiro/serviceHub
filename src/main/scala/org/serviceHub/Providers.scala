package org.serviceHub

import org.serviceHub.domain.Service
import org.serviceHub.providers.queue.{MQProvider, RabbitMQProvider}
import org.serviceHub.providers.storage.{StorageProvider, MongoStorageProvider}
import spray.http.Uri

object Providers {
  def createMQProvider(service: Service): MQProvider = {
    Uri(service.queue) match {
      case Uri("rabbitmq", authority, _, _, _) => new RabbitMQProvider(service)
    }
  }

  def createStorageProvider(service: Service): StorageProvider = {
    Uri(service.storage) match {
      case Uri("mongodb", authority, _, _, _) => new MongoStorageProvider(service)
    }
  }
}
