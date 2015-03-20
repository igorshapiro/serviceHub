package actors

import akka.pattern.ask
import helpers.{ActorSpecBase, RabbitMQTestHelper, TestServices}
import org.scalatest.BeforeAndAfterEach
import org.serviceHub.actors.queue.MQActor._
import org.serviceHub.actors.queue.{MQActor, ServiceExtensions}
import org.serviceHub.domain.ServicesRepository

class RabbitMQActorTest extends ActorSpecBase with TestServices with BeforeAndAfterEach with RabbitMQTestHelper {
  import ServiceExtensions._

  implicit val repository = new ServicesRepository(ordersService)

  "! Consume" should "start forwarding messages to calling actor" in {
    enqueue(ordersService.inputQueue, MQActor.asJsonString(orderCreatedMsg))

    queueActor ! Consume(ordersService, InputQueue, testActor)
    expectMsg(MessageArrived(orderCreatedMsg, ordersService))
  }

  "! Enqueue" should "send the message to queue" in {
    val enqueued = (queueActor ? Enqueue(ordersService, InputQueue, orderCreatedMsg)).mapTo[MessageEnqueued]
    whenReady(enqueued) { x => x.message should be (orderCreatedMsg)}

    ordersService should haveInInputQueue(orderCreatedMsg)
  }
}
