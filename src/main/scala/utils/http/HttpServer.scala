package utils.http

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.http.HttpResponse
import utils.http.ControlActor.Stop
import utils.http.HttpServer.RequestHandler

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
//import scala.concurrent.ExecutionContext.implicits.global

class HttpServer(port: Integer = 8080)(implicit system: ActorSystem) {

  val controlActor = system.actorOf(Props(new ControlActor()))
  def start(handler: RequestHandler) = {
    val handlerProps = Props(new TestHandlerActor(handler))
    val handlerActor = system.actorOf(handlerProps)
    IO(Http).tell(Http.Bind(handlerActor, interface = "localhost", port = port), controlActor)
    this
  }

  def stop(sync: Boolean = true): Unit = {
    implicit val stopTimeout = Timeout(5 seconds)
    if (sync)
      Await.result((controlActor ? Stop()).mapTo[Boolean], stopTimeout.duration)
  }
}

object HttpServer {
  type RequestHandler = PartialFunction[Any, Future[HttpResponse]]
}