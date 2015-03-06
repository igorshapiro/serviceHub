package integration

import akka.actor._
import akka.util.Timeout
import helpers.{SpecBase, SyncHttp}
import org.serviceHub.ServiceHub
import org.serviceHub.domain.{Endpoint, Message, Service}
import spray.http.HttpMethods._
import spray.http._
import utils.http.HttpServer

import scala.concurrent.duration._

class ServiceHubTest extends SpecBase with SyncHttp {
  implicit val system = ActorSystem("ServiceHubTestSystem")
  implicit val timeout = Timeout(5 seconds)

  val ordersService = Service("orders", Seq("order_created"), Seq.empty, Seq(
    Endpoint("http://localhost:8081/orders/:type")
  ))

  val billingService = Service("billing", Seq.empty, Seq("order_created"), Seq(
    Endpoint("http://localhost:8081/billing/:type")
  ))

  val bamService = Service("bam", Seq.empty, Seq("order_created"), Seq(
    Endpoint("http://localhost:8081/bam/:env/:type")
  ))

  "POST /api/v1/messages" should "deliver message to subscribers" in {
    var deliveredToBilling = 0
    var deliveredToBAM = 0

    val hub = new ServiceHub(ordersService, billingService, bamService)
    whenReady(hub.initialized) {x => }
    val service = new HttpServer(8081).start({
      case HttpRequest(POST, Uri.Path("/billing/order_created"), _, _, _) =>
        deliveredToBilling += 1
        HttpResponse(200)
      case HttpRequest(POST, Uri.Path("/bam/default/order_created"), _, _, _) =>
        deliveredToBAM += 1
        HttpResponse(500)
    })
    try{
      sendMessage(Message("order_created", maxAttempts = 6))
      Thread.sleep(100)
      deliveredToBAM should be (6)
      deliveredToBilling should be (1)
    }
    finally {
      whenReady(service.stop()) { x => x should be (true) }
      whenReady(hub.stop()) { x => x should be (true) }
    }
  }
}






