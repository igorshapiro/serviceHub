package integration

import akka.actor._
import akka.util.Timeout
import helpers.{SpecBase, SyncHttp, TestServices}
import org.serviceHub.ServiceHub
import org.serviceHub.domain.Message
import spray.http.HttpMethods._
import spray.http._
import utils.http.HttpServer

import scala.concurrent.Future
import scala.concurrent.duration._

class ServiceHubTest extends SpecBase with SyncHttp with TestServices {
  implicit val system = ActorSystem("ServiceHubTestSystem")
  implicit val timeout = Timeout(5 seconds)

  "POST /api/v1/messages" should "deliver message to subscribers" in {
    var deliveredToBilling = 0
    var deliveredToBAM = 0

    val hub = new ServiceHub(ordersService, billingService, bamService)
    val service = new HttpServer(8081).start({
      case HttpRequest(POST, Uri.Path("/billing/order_created"), _, _, _) =>
        deliveredToBilling += 1
        Future.successful(HttpResponse(200))
      case HttpRequest(POST, Uri.Path("/bam/default/order_created"), _, _, _) =>
        deliveredToBAM += 1
        Future.successful(HttpResponse(500))
    })
    try{
      sendMessage(Message("order_created", maxAttempts = 6))
      Thread.sleep(300)
      deliveredToBAM should be (6)
      deliveredToBilling should be (1)
    }
    finally {
      whenReady(hub.stop()) { x => x should be (true) }
      service.stop(true)
    }
  }
}
