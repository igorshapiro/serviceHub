package utils.http

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.testkit.TestKit
import akka.util.Timeout
import helpers.{SpecBase, SyncHttp}
import org.scalamock.MockFunction0
import org.scalatest.concurrent._
import org.scalatest.time.{Millis, Seconds, Span}
import spray.can.Http
import spray.can.Http.ConnectionAttemptFailedException
import spray.http.HttpMethods._
import spray.http.{HttpRequest, HttpResponse, Uri}
import utils.http.ControlActor.Stop
import utils.http.HttpServer.RequestHandler

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class TestHandlerActor(handler: RequestHandler) extends Actor with ActorLogging {
  def defaultHandler: PartialFunction[Any, Unit] = {
    // On connection - register myself as listener for request messages
    case _: Http.Connected => sender ! Http.Register(self)
  }

  // Forward http requests to user-provided handler
  def userHandler: PartialFunction[Any, Unit] = {
    case r: HttpRequest => sender ! handler(r)
  }
  override def receive = defaultHandler orElse userHandler
}

object ControlActor {
  case class Stop()
}

class ControlActor(implicit system: ActorSystem) extends Actor with ActorLogging {
  import context._
  var listener: ActorRef = null
  var shuttingDownPromise = Promise[Boolean]

  override def receive: Receive = {
    case Http.Bound(_) =>
      log.debug("***SERVICEHUB bound...")
      listener = sender
    case Stop() =>
      val _sender = sender
      implicit val timeout = Timeout(5 seconds)
      listener ! Http.Unbind(1 second)
      shuttingDownPromise.future.onComplete {
        case Success(_) =>
          _sender ! true
          context.stop(self)
        case Failure(_) =>
          _sender ! false
          context.stop(self)
      }
    case Http.Unbound =>
      log.debug("SERVICEHUB1: Actor STOPPED")
      shuttingDownPromise.success(true)
  }
}

object HttpServer {
  type RequestHandler = PartialFunction[Any, HttpResponse]
}

class HttpServer(port: Integer = 8080)(implicit system: ActorSystem) {
  val controlActor = system.actorOf(Props(new ControlActor()))
  def start(handler: RequestHandler) = {
    val handlerProps = Props(new TestHandlerActor(handler))
    val handlerActor = system.actorOf(handlerProps)
    IO(Http).tell(Http.Bind(handlerActor, interface = "localhost", port = port), controlActor)
    this
  }

  def stop(sync: Boolean = true): Future[Boolean] = {
    implicit val stopTimeout = Timeout(5 seconds)
    (controlActor ? Stop()).mapTo[Boolean]
  }
}

class HttpServerTest extends SpecBase with SyncHttp with ScalaFutures {
  implicit val system = ActorSystem("HttpServerTest")
  implicit val timeout = Timeout(1 second)
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(150, Millis))

  def withServerCallingMock(mockToCall: MockFunction0[Unit])(block: () => Any): Unit = {
    val httpServer = new HttpServer(8080).start({
      case HttpRequest(GET, Uri.Path("/handler"), _, _, _) => mockToCall(); HttpResponse(200);
    })
    try {
      block()
    }
    finally {
      whenReady(httpServer.stop()) {stopped =>
        stopped should be (true)
        println("HTTP server stopped")
      }
    }
  }

  "start" should "start a server" in {
    val shouldBeCalled = mockFunction[Unit]("shouldBeCalled")
    shouldBeCalled.expects().once()
    withServerCallingMock(shouldBeCalled) { () =>
      HttpGet("http://localhost:8080/handler")
    }
  }

  "stop" should "stop the server" in {
    val shouldBeCalledOnce = mockFunction[Unit]("shouldBeCalledOnce")
    shouldBeCalledOnce.expects().once()
    withServerCallingMock(shouldBeCalledOnce) { () =>
      HttpGet("http://localhost:8080/handler")
    }
    a [ConnectionAttemptFailedException] should be thrownBy HttpGet("http://localhost:8080/handler")
  }

  override protected def afterAll = {
    TestKit.shutdownActorSystem(system)
  }
}
