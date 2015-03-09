package actors

import akka.testkit.TestActorRef
import helpers.ActorSpecBase
import org.serviceHub.actors.FetchingActor
import org.serviceHub.domain.{Message, Service}

import scala.concurrent.Promise

class FetchingActorTest extends ActorSpecBase {
  val service = Service("billing", subscribes = Seq("order_created"))

  override protected def beforeEach(): Unit = service.purgeQueuesAndStorages
  override protected def afterEach(): Unit = service.stopConsumers

  "actor" should "dispatch messages to Processing actors" in {

    val message = Message("order_created")
    service.enqueueInput(message)

    val actor = TestActorRef(new FetchingActor(service, testActor))

    val receivedMessage = Promise[Message]
    expectMsg(message)
  }
}
