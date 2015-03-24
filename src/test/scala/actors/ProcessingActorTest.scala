package actors

import akka.testkit.TestActorRef
import helpers.{TestServices, MongoTestHelper, RabbitMQTestHelper, ActorSpecBase}
import org.serviceHub.Providers
import org.serviceHub.actors.ProcessorActor
import org.serviceHub.actors.ProcessorActor.DeliverMessage
import org.serviceHub.domain.{ServicesRepository, Endpoint, Message, Service}
import spray.http.HttpMethods._
import spray.http.{HttpRequest, HttpResponse, Uri}
import spray.json._
import utils.http.HttpServer

import scala.concurrent.{Future, Promise}

class ProcessingActorTest extends ActorSpecBase with RabbitMQTestHelper with MongoTestHelper with TestServices {
  import org.serviceHub.domain.MessageJsonProtocol._

  val service = Service("billing",
    subscribes = Seq("order_created"),
    endpoints = Seq(Endpoint("http://localhost:8080/:type")),
    queue = rabbitMQTestUrl
  )

  val repository = new ServicesRepository(service)

  def runScenario(msg: Message, handler: Message => HttpResponse, block: () => Unit) = {
    val path = s"/${msg.messageType}"
    val httpServer = new HttpServer().start {
      case HttpRequest(POST, Uri.Path(path), _, e, _) =>
        val msg = e.asString.parseJson.convertTo[Message]
        Future.successful(handler(msg))
    }
    try {
      val actor = TestActorRef(new ProcessorActor(queueActor, storageActor))
      actor ! DeliverMessage(msg, service)

      block()
    }
    catch{
      case x: Exception => throw x
    }
    finally {
      httpServer.stop(true)
    }
  }

  "processor" should "send the message to service" in {
    val called = Promise[Message]
    val msg = Message("order_created")

    runScenario(msg, msg => {
      called.success(msg)
      HttpResponse(200)
    }, { () =>
      whenReady(called.future) { m => m should be (msg) }
    })
  }

  "processor" should "re-enqueue the message if attempts < maxAttempts" in {
    val deliveredMessage = Promise[Message]
    val msg = Message("order_created", maxAttempts = 5, attemptsMade = 3)

    runScenario(msg, msg => {
      deliveredMessage.success(msg)
      HttpResponse(500)
    }, { () =>
      Thread.sleep(100)
      deliveredMessage.isCompleted should be (true)
    })

    service should haveInInputQueue(msg.attempted)
  }

  "processor" should "move the message to dead storage (and not re-enqueue) if attempts becomes maxAttempts" in {
    val called = Promise[Message]
    val msg = Message("order_created", maxAttempts = 5, attemptsMade = 4)

    runScenario(msg, msg => {
      called.success(msg)
      HttpResponse(500)
    }, { () =>
      Thread.sleep(100)
      called.isCompleted should be (true)
    })

    // Check that the message moved to dead storage
    val deadMessages = Providers.createStorageProvider(service).getDeadMessages
    deadMessages.length should be (1)
    deadMessages.head should be (msg.attempted)

    service shouldNot haveInInputQueue(msg)
    service shouldNot haveInInputQueue(msg.attempted)
  }
}





