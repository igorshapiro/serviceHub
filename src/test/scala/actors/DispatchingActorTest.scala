package actors

import akka.actor.Actor
import akka.testkit.TestActorRef
import helpers.ActorSpecBase
import org.serviceHub.domain.{Message, Service, ServicesRepository}

import scala.concurrent.Promise

class DispatchingActorTest extends ActorSpecBase {
  val orders = Service("orders", publishes = Seq("order_created"))
  val billing = Service("billing", subscribes = Seq("order_created"))
  val bam = Service("bam", subscribes = Seq("*"))

  val repo = new ServicesRepository(orders, billing, bam)

  override protected def beforeEach(): Unit = List(orders, billing, bam).foreach(_.purgeAllQueues)
  override protected def afterEach(): Unit = repo.stopAllServices

  "it" should "dispatch outgoing messages to subscriber input queues" in {
    val msg = Message("order_created")
    orders.enqueueOutgoing(msg)

    val dispatcher = TestActorRef(new DispatchingActor(orders, repo))

    val bamReceived = Promise[Message]
    val billingReceived = Promise[Message]

    bam.consumeInput(msg => bamReceived.success(msg))
    billing.consumeInput(msg => billingReceived.success(msg))

    whenReady(bamReceived.future) { m => m should be (msg)}
    whenReady(billingReceived.future) { m => m should be (msg)}
  }
}

class DispatchingActor(service: Service, repository: ServicesRepository) extends Actor {

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    val _self = self
    service.consumeOutgoing(msg => _self ! msg)
  }

  def dispatch(message: Message): Unit = {
    repository.getSubscribersFor(message).foreach( sub =>
      sub.enqueueInput(message)
    )
  }

  override def receive: Receive = {
    case msg: Message => dispatch(msg)
  }
}
