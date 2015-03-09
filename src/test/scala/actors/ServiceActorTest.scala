package actors

import akka.actor.ActorRef
import akka.testkit.{TestProbe, TestActorRef}
import helpers.ActorSpecBase
import org.serviceHub.actors.QueueMessages.{Initialized, Enqueue, Initialize}
import org.serviceHub.actors.ServiceActor
import org.serviceHub.actors.ServiceActor.Send
import org.serviceHub.domain.{Message, Service, ServicesRepository}

class ServiceActorTest extends ActorSpecBase {
  val msg = Message("order_created")
  val service = Service("orders")
  val repo = new ServicesRepository(service)

  def initializedSvcActor(probe: TestProbe): ActorRef = {
    val svcActor = TestActorRef(new ServiceActor(service, repo,
      _outgoingQueueActor = if (probe == null) testActor else probe.ref
    ))
    svcActor ! ServiceActor.Initialize()
    probe.expectMsg(Initialize(service))
    probe.reply(Initialized())

    svcActor
  }

  "! Initialize" should "initialize outgoing queue actor" in {
    val svcActor = TestActorRef(new ServiceActor(service, repo, _outgoingQueueActor = testActor))
    svcActor ! ServiceActor.Initialize()
    expectMsg(Initialize(service))
    lastSender ! Initialized()
  }

  "! Send(msg)" should "send message to outgoing queue" in {
    val probe = new TestProbe(system)
    val svcActor = initializedSvcActor(probe)

    svcActor ! Send(msg)
    probe.expectMsg(Enqueue(msg))
  }
}

