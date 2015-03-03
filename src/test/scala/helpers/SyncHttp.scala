package helpers

import akka.actor.ActorSystem
import akka.util.Timeout
import org.serviceHub.domain.Message
import spray.client.pipelining._

import scala.concurrent.Await
import scala.concurrent.duration._

trait SyncHttp {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val system: ActorSystem
  implicit val timeout: Timeout

  def sendMessage(msg: Message) = {
    import org.serviceHub.domain.MessageJsonProtocol._

    val pipeline = addHeader("Connection", "close") ~> sendReceive
    val future = pipeline {
      Post("http://localhost:8080/api/v1/messages", msg)
    }
  }

  def HttpGet(url: String) = {
    val pipeline = addHeader("Connection", "close") ~> sendReceive
    val responseFuture = pipeline {
      Get(url)
    }
    Await.result(responseFuture, 1 second)
  }
}
