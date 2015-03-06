package org.serviceHub

import _root_.utils.http.HttpServer
import akka.actor.{Props, ActorRef, ActorSystem}
import akka.util.Timeout
import akka.pattern.ask
import org.serviceHub.actors.ServiceActor
import org.serviceHub.actors.ServiceActor.{Initialize, Initialized, Handle}
import org.serviceHub.domain.{Message, Service, ServicesRepository}
import spray.http.HttpMethods._
import spray.http.{HttpCharsets, HttpRequest, HttpResponse, Uri}
import spray.json._

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

class ServiceHub(services: Service*)(implicit system: ActorSystem) {
  import scala.concurrent.ExecutionContext.Implicits.global
  import org.serviceHub.domain.MessageJsonProtocol._

  implicit val timeout = Timeout(120 seconds)

  val servicesRespository = new ServicesRepository(services:_*)

  val apiServer = new HttpServer(8080).start({
    case HttpRequest(POST, Uri.Path("/api/v1/messages"), _, entity, _) =>
      val msg = entity.asString(HttpCharsets.`UTF-8`).parseJson.convertTo[Message]
      deliverMessage(msg)
      HttpResponse(200)
  })

  def deliverMessage(msg: Message): Unit = {
    val subscribers = servicesRespository.getSubscribersFor(msg)
    for (
      subscriber <- subscribers;
      actor = serviceActorsMap(subscriber)
    ) actor ! Handle(msg)

  }

  def createServiceActor(service: Service) = system.actorOf(
    Props(new ServiceActor(service, servicesRespository)), s"service-${service.name}"
  )

  val serviceActorsMap: Map[Service, ActorRef] = services.map(svc => (svc, createServiceActor(svc))).toMap

  val initialized: Future[Seq[Initialized]] = Future.sequence(
    serviceActorsMap.values.map{a => (a ? Initialize()).mapTo[Initialized] }.toSeq
  )

  def stop(): Future[Boolean] = {
    val stop = Promise[Boolean]
    apiServer.stop() onComplete {
      _ => stop.success(true)
    }
    stop.future
  }
}
