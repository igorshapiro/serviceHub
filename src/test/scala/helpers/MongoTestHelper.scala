package helpers

import akka.actor.ActorRef
import akka.testkit.{TestActorRef, TestKitBase}
import com.mongodb.casbah.MongoClient
import org.scalatest.{Args, Status, Suite}
import org.serviceHub.actors.storage.{MongoStorageActor, StorageActor}
import org.serviceHub.actors.storage.StorageActor.{StopAll, DeadStorage}
import org.serviceHub.domain.{Message, Service, ServicesRepository}
import org.serviceHub.providers.storage.MongoUtils

trait MongoTestHelper extends Suite with TestKitBase {
  implicit val repository: ServicesRepository

  val mongoClient = MongoClient()
  val mongoDb = mongoClient(MongoUtils.SERVICE_HUB_DB)

  protected var storageActor: ActorRef = null

  override def run(testName: Option[String], args: Args): Status = {
    repository.services.foreach { svc =>
      mongoDb(StorageActor.resolveStorageName(svc, DeadStorage)).dropCollection()
    }

    storageActor = TestActorRef[MongoStorageActor]
    try{
      super.run(testName, args)
    }
    finally {
      storageActor ! StopAll()
    }
  }

  class ServiceExtensions(svc: Service) {
    def deadStorage = StorageActor.resolveStorageName(svc, DeadStorage)
    def deadMessages: List[Message] = {
      val q = mongoDb(deadStorage).find()
      val messages = for (obj <- q; msg = MongoUtils.msgFromDBObject(obj)) yield msg
      messages.toList
    }
  }

  implicit def serviceExtensions(svc: Service): ServiceExtensions = new ServiceExtensions(svc)
}
