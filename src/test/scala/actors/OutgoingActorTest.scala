package actors

import akka.pattern.ask
import akka.testkit.TestActorRef
import helpers.ActorSpecBase
import org.serviceHub.actors.OutgoingActor
import org.serviceHub.actors.QueueMessages.{Enqueue, Enqueued}
import org.serviceHub.domain.{Message, Service, ServicesRepository}

import scala.concurrent.Promise

class OutgoingActorTest extends ActorSpecBase {
  val service = Service("orders", publishes = Seq("order_created"))
  val repo = new ServicesRepository(service)

  override protected def beforeEach(): Unit = repo.purgeAllQueuesForAllServices
  override protected def afterEach(): Unit = service.stopConsumers

  "! Enqueue(msg)" should "put the message in service outgoing queue" in {
    val actor = TestActorRef(new OutgoingActor(repo))
    val msg = Message("order_created")
    val reply = (actor ? Enqueue(msg)).mapTo[Enqueued]
    whenReady(reply) { x => x shouldNot be (null) }

    val receivedMsg = Promise[Message]
    service.consumeOutgoing(m => {receivedMsg.success(m)})
    whenReady(receivedMsg.future) {m => m should be (msg)}
  }
}


