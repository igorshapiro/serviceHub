package utils.http

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.http.HttpResponse
import utils.http.ControlActor.Stop
import utils.http.HttpServer.RequestHandler

import scala.concurrent.Future
import scala.concurrent.duration._

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

object HttpServer {
  type RequestHandler = PartialFunction[Any, HttpResponse]
}