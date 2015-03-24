package org.serviceHub

import _root_.utils.http.HttpServer
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import org.serviceHub.actors.QueueMessages.{Enqueue, Enqueued}
import org.serviceHub.actors.queue.RabbitMQActor
import org.serviceHub.actors.storage.MongoStorageActor
import org.serviceHub.actors.{DispatchingActor, FetchingActor, OutgoingActor, ProcessorActor}
import org.serviceHub.domain.{Message, Service, ServicesRepository}
import spray.http.HttpMethods._
import spray.http.{HttpCharsets, HttpRequest, HttpResponse, Uri}
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class ServiceHub(services: Service*)(implicit system: ActorSystem) {
  import org.serviceHub.domain.MessageJsonProtocol._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout = Timeout(120 seconds)

  val servicesRepository = new ServicesRepository(services:_*)

  val apiServer = new HttpServer(8080).start({
    case HttpRequest(POST, Uri.Path("/api/v1/messages"), _, entity, _) =>
      val msg = entity.asString(HttpCharsets.`UTF-8`).parseJson.convertTo[Message]
      val enqueued = (outgoingRouter ? Enqueue(msg)).mapTo[Enqueued]
      val response = Promise[HttpResponse]()
      enqueued.onComplete {
        case Success(e: Enqueued) => response.success(HttpResponse(200))
        case Failure(_) => response.failure(new Exception("Unable to handle request"))
      }
      response.future
  })

  val mongoStorageProps = Props[MongoStorageActor]
  val mongoStorageRouter = system.actorOf(RoundRobinPool(2).props(mongoStorageProps), "mongo-storage-router")

  val rabbitMQProps = Props[RabbitMQActor]
  val rabbitMQRouter = system.actorOf(RoundRobinPool(2).props(rabbitMQProps), "rabbitmq-actor-router")

  val outgoingProps = Props.create(classOf[OutgoingActor], servicesRepository, rabbitMQRouter)
  val outgoingRouter = system.actorOf(RoundRobinPool(2).props(outgoingProps), "outgoing-actors-router")
  val processorProps = Props(classOf[ProcessorActor], rabbitMQRouter, mongoStorageRouter)
  val processorsRouter = system.actorOf(RoundRobinPool(100).props(processorProps), "processor-actors-router")
  val fetchers = services.map(s => system.actorOf(Props(classOf[FetchingActor], servicesRepository, processorsRouter, rabbitMQRouter)))
  val dispatcherProps = Props(classOf[DispatchingActor], servicesRepository, rabbitMQRouter)
  val dispatcherRouter = system.actorOf(RoundRobinPool(2).props(dispatcherProps), "dispatching-actors-router")

  def stop(): Future[Boolean] = {
    apiServer.stop(true)
    Future { true }
  }
}
