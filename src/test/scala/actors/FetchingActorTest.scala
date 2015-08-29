package actors

import akka.testkit.TestActorRef
import helpers.{ActorSpecBase, RabbitMQTestHelper, TestServices}
import org.serviceHub.actors.FetchingActor
import org.serviceHub.actors.ProcessorActor.DeliverMessage
import org.serviceHub.actors.queue.MQActor
import org.serviceHub.actors.queue.MQActor.InputQueue
import org.serviceHub.domain.ServicesRepository

class FetchingActorTest extends ActorSpecBase with RabbitMQTestHelper with TestServices {
  val repository = new ServicesRepository(billingService)

  "actor" should "dispatch messages to Processing actors" in {
    TestActorRef(new FetchingActor(repository, testActor, queueActor))
    enqueue(MQActor.resolveQueueName(billingService, InputQueue), orderCreatedMsg)
    expectMsgPF() {
      case DeliverMessage(msg, svc, _) =>
        msg should be(orderCreatedMsg)
        svc should be(billingService)
    }
  }
}
