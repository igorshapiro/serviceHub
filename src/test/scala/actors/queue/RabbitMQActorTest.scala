package actors.queue

import akka.pattern.ask
import helpers.{ActorSpecBase, RabbitMQTestHelper, TestServices}
import org.scalatest.BeforeAndAfterEach
import org.serviceHub.actors.queue.MQActor._
import org.serviceHub.actors.queue.ServiceExtensions
import org.serviceHub.domain.ServicesRepository

class RabbitMQActorTest extends ActorSpecBase with TestServices with BeforeAndAfterEach with RabbitMQTestHelper {
  import ServiceExtensions._

  implicit val repository = new ServicesRepository(ordersService)

//  "! Consume" should "not acknowledge messages automatically" in {
//    enqueue(ordersService.inputQueue, orderCreatedMsg)
//
//    queueActor ! Consume(ordersService, InputQueue, testActor)
//    ordersService should haveUnacknowledgedInputMessages(orderCreatedMsg)
//
//    expectMsg(MessageArrived(orderCreatedMsg, ordersService))
//
//    queueActor ! Acknowledge(msg)
//    ordersService should haveUnacknowledgedInputMessages(List.empty)
//  }

  "! Consume" should "start forwarding messages to calling actor" in {
    enqueue(ordersService.inputQueue, orderCreatedMsg)

    queueActor ! Consume(ordersService, InputQueue, testActor)
    expectMsgPF() {
      case MessageArrived(msg, svc, _) =>
        msg should be(orderCreatedMsg)
        svc should be(ordersService)
    }
  }

  "! Enqueue" should "send the message to queue" in {
    val enqueued = (queueActor ? Enqueue(ordersService, InputQueue, orderCreatedMsg)).mapTo[MessageEnqueued]
    whenReady(enqueued) { x => x.message should be (orderCreatedMsg)}

    ordersService should haveInInputQueue(orderCreatedMsg)
  }
}
