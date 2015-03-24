package org.serviceHub.actors.storage

import akka.actor.FSM
import org.serviceHub.actors.storage.StorageActor._

class MongoStorageActor extends FSM[State, StorageActorData] {
  startWith(Active, MongoServices())

  when(Active) {
    case Event(Store(msg, svc, storageType), data: MongoServices) =>
      val newData = data.withService(svc)
      newData(svc).store(msg, storageType)
      sender() ! Stored(msg)
      stay using newData
  }

  initialize()
}
