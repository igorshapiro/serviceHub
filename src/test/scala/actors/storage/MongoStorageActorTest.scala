package actors.storage

import akka.pattern.ask
import helpers.{ActorSpecBase, MongoTestHelper, TestServices}
import org.serviceHub.actors.storage.StorageActor._
import org.serviceHub.domain.ServicesRepository

class MongoStorageActorTest extends ActorSpecBase with MongoTestHelper with TestServices {
  val repository = new ServicesRepository(ordersService)

  "Store(msg, service, type)" should "add the message to corresponding storage" in {
    val stored = (storageActor ? Store(orderCreatedMsg, ordersService, DeadStorage)).mapTo[Stored]
    whenReady(stored) { _ =>
      ordersService.deadMessages should be(List(orderCreatedMsg))
    }
  }
}
