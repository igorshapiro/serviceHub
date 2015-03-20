package actors

import akka.pattern.ask
import akka.testkit.TestActorRef
import helpers.{ActorSpecBase, RabbitMQTestHelper, TestServices}
import org.serviceHub.actors.OutgoingActor
import org.serviceHub.actors.QueueMessages.{Enqueue, Enqueued}
import org.serviceHub.domain.ServicesRepository

class OutgoingActorTest extends ActorSpecBase with RabbitMQTestHelper with TestServices {
  val repository = new ServicesRepository(ordersService)

  "! Enqueue(msg)" should "put the message in service outgoing queue" in {
    val actor = TestActorRef(new OutgoingActor(repository, queueActor))   // TODO: Fix
    val reply = (actor ? Enqueue(orderCreatedMsg)).mapTo[Enqueued]
    whenReady(reply) { x => x shouldNot be (null) }

    ordersService should haveInOutgoingQueue(orderCreatedMsg)
  }
}


