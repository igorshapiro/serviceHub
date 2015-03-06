package utils.http

import akka.actor.{ActorRef, ActorSystem, ActorLogging, Actor}
import akka.util.Timeout
import spray.can.Http
import utils.http.ControlActor.Stop

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ControlActor(implicit system: ActorSystem) extends Actor with ActorLogging {
  import context._
  var listener: ActorRef = null
  var shuttingDownPromise = Promise[Boolean]

  override def receive: Receive = {
    case Http.Bound(_) =>
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
      shuttingDownPromise.success(true)
  }
}

object ControlActor {
  case class Stop()
}