package actors

import akka.actor.Props
import helpers.{ActorSpecBase, RabbitMQTestHelper, TestServices}
import org.serviceHub.actors.DispatchingActor
import org.serviceHub.domain.{Message, ServicesRepository}

class DispatchingActorTest extends ActorSpecBase with TestServices with RabbitMQTestHelper {
  import org.serviceHub.actors.queue.ServiceExtensions._

  val repository = new ServicesRepository(ordersService, billingService, bamService)

  val orderCreated = Message("order_created")
  val orderPaid = Message("order_paid")

  "it" should "dispatch outgoing messages to subscriber input queues" in {
    enqueue(ordersService.outgoingQueue, orderCreated)
    enqueue(billingService.outgoingQueue, orderPaid)

    val actor = system.actorOf(Props(new DispatchingActor(repository, queueActor)))

    billingService should haveInInputQueue (orderCreated)
    ordersService should haveInInputQueue (orderPaid)
    bamService should haveInInputQueue (orderCreated, orderPaid)
  }

  "it" should "run next test" in {
    println("in run next test")
  }
}


