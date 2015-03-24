package utils.http

import akka.actor._
import akka.testkit.TestKit
import akka.util.Timeout
import helpers.{SpecBase, SyncHttp}
import org.scalamock.MockFunction0
import spray.can.Http.ConnectionAttemptFailedException
import spray.http.HttpMethods._
import spray.http.{HttpRequest, HttpResponse, Uri}

import scala.concurrent.Future
import scala.concurrent.duration._

class HttpServerTest extends SpecBase with SyncHttp {
  implicit val system = ActorSystem("HttpServerTest")
  implicit val timeout = Timeout(1 second)

  def withServerCallingMock(mockToCall: MockFunction0[Unit])(block: () => Any): Unit = {
    val httpServer = new HttpServer(8080).start({
      case HttpRequest(GET, Uri.Path("/handler"), _, _, _) => mockToCall(); Future.successful(HttpResponse(200));
    })
    try {
      block()
    }
    finally {
      httpServer.stop(true)
      println("HTTP server stopped")
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
