//package integration
//
//import java.util.concurrent.atomic.AtomicInteger
//
//import helpers.{ActorSpecBase, SyncHttp}
//import org.scalatest.DoNotDiscover
//import org.serviceHub.ServiceHub
//import org.serviceHub.domain.{Endpoint, Message, Service}
//import spray.http.HttpMethods._
//import spray.http.{HttpRequest, HttpResponse, Uri}
//import utils.http.HttpServer
//
//import scala.concurrent.Future
//
//@DoNotDiscover
//class PerformanceTest extends ActorSpecBase with SyncHttp {
//  val orders = Service("orders", publishes = Seq("order_created"))
//  val billing = Service("billing", subscribes = Seq("order_created"), endpoints = Seq(
//    Endpoint("http://localhost:8081/:type")
//  ))
//
//  "it" should "be fast" in {
//    val testLengthSeconds = 3
//    var delivered = new AtomicInteger(0)
//    val hub = new ServiceHub(orders, billing)
//
//    val service = new HttpServer(8081).start({
//      case HttpRequest(POST, Uri.Path("/order_created"), _, _, _) =>
//        delivered.incrementAndGet()
//        Future.successful(HttpResponse(200))
//    })
//    Thread.sleep(1000)
//    try {
//      for (i <- 0 to 30000) sendMessage(Message("order_created"), sync = false)
//      Thread.sleep(1000 * testLengthSeconds)
//    }
//    catch {
//      case x => println(x)
//    }
//    finally {
//      service.stop()
//      whenReady(hub.stop()) { x => x should be (true) }
//    }
//
//    val rate = 1.0 * delivered.get() / testLengthSeconds
//    println(s"${delivered.get()} messages delivered at rate $rate messages/sec")
//  }
//}
