package org.serviceHub.actors.storage

import com.mongodb.casbah.MongoClient
import org.serviceHub.domain.{Message, Service}
import org.serviceHub.providers.storage.MongoUtils

object StorageActor {
  def resolveStorageName(svc: Service, storageType: StorageType): String = storageType match {
    case DeadStorage => s"${svc.name}_dead"
  }

  trait StorageType
  object DeadStorage extends StorageType

  trait State
  object Active extends State

  trait StorageActorData
  case class MongoServices(services: Map[Service, MongoService] = Map[Service, MongoService]()) extends StorageActorData {
    def apply(svc: Service) = services(svc)

    def withService(svc: Service) = {
      if (!services.contains(svc)) {
        val mongoService = new MongoService(svc)
        val mapWithNewService = services + ((svc, mongoService))
        MongoServices(mapWithNewService)
      }
      else this
    }
  }

  case class MongoService(service: Service) {
    val client = MongoClient()
    val db = client(MongoUtils.SERVICE_HUB_DB)

    def store(msg: Message, storageType: StorageType) = {
      val storageName = StorageActor.resolveStorageName(service, storageType)
      db(storageName).insert(MongoUtils.msgToDBObject(msg))
    }
  }

  case class Store(msg: Message, svc: Service, storageType: StorageType)
  case class Stored(msg: Message)
  case class StopAll()
}
