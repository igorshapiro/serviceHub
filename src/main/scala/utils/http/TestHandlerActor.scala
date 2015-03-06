package utils.http

import akka.actor.{Actor, ActorLogging}
import spray.can.Http
import spray.http.HttpRequest
import utils.http.HttpServer.RequestHandler

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
