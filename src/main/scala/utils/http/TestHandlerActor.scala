package utils.http

import akka.actor.{Actor, ActorLogging}
import spray.can.Http
import spray.http.{HttpResponse, HttpRequest}
import utils.http.HttpServer.RequestHandler

import scala.util.{Failure, Success}

class TestHandlerActor(handler: RequestHandler) extends Actor with ActorLogging {
  import context._

  def defaultHandler: PartialFunction[Any, Unit] = {
    // On connection - register myself as listener for request messages
    case _: Http.Connected => sender ! Http.Register(self)
  }

  // Forward http requests to user-provided handler
  def userHandler: PartialFunction[Any, Unit] = {
    case r: HttpRequest =>
      val _sender = sender()
      handler(r) onComplete {
        case Success(res) => _sender ! res
        case Failure(err) =>
          log.error(err, "Error handling request " + r)
          _sender ! HttpResponse(500)
      }
  }
  override def receive = defaultHandler orElse userHandler
}
