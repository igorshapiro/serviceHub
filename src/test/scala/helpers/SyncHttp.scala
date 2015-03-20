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

  def sendMessage(msg: Message, sync: Boolean = true)(implicit hubUrl: String = "http://localhost:8080") = {
    import org.serviceHub.domain.MessageJsonProtocol._

//    val pipeline = addHeader("Connection", "close") ~> sendReceive
    val pipeline = sendReceive
    val future = pipeline {
      Post(s"$hubUrl/api/v1/messages", msg)
    }
    if (sync) Await.result(future, 1 second)
  }

  def HttpGet(url: String) = {
    val pipeline = addHeader("Connection", "close") ~> sendReceive
    val responseFuture = pipeline {
      Get(url)
    }
    Await.result(responseFuture, 1 second)
  }
}
