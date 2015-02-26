import spray.json.{JsObject, JsValue}

case class Endpoint(url: String, env: String = "*")
case class Service(name: String,
                   publishes: Seq[String] = Seq.empty,
                   subscribes: Seq[String] = Seq.empty,
                   endpoints: Seq[Endpoint] = Seq(Endpoint("http://localhost:8080/events/:message_type"))) {
  def isSubscriberOf(msg: Message) = subscribes.contains(msg.messageType) || subscribes.contains("*")
  def isPublisherOf(msg: Message) = publishes.contains(msg.messageType)
}
case class Message(
  messageType: String,
  content: JsValue = new JsObject(Map.empty),
  attemptsMade: Int = 0,
  maxAttempts: Int = 5,
  env: String = "default"
)(implicit val context: HubContext)

case class NoAuthorityForMessageException(messageType: String, publishingServices: Seq[Service])
  extends Exception(s"No authority for message $messageType: " +
    s"it's published by more than one service: ${publishingServices.map(_.name).mkString(", ")}")

class ServicesRepository(services: Service*) {
  detectNonAuthoritativeMessages

  def getSubscribersFor(msg: Message) = services.filter(_.isSubscriberOf(msg))
  def getPublisherOf(msg: Message) = services.find(_.isPublisherOf(msg))

  def detectNonAuthoritativeMessages = {
    val messagePublishers = services.foldLeft(Map[String, List[Service]]())((acc, svc) =>
      svc.publishes.foldLeft(acc)((acc, msg) =>
        if (!acc.contains(msg))
          acc.updated(msg, List(svc))
        else
          acc.updated(msg, acc(msg) ++ List(svc))
      )
    )
    messagePublishers.find(_._2.length > 1) match {
      case Some((msg, services)) => throw new NoAuthorityForMessageException(msg, services)
      case None =>
    }
  }
}

class HubContext(repository: ServicesRepository) {
}

class ServiceRepositoryTest extends SpecBase {
  val ordersService = Service("orders", subscribes = Seq("order_paid"), publishes = Seq("order_created"))
  val emailService = Service("email", subscribes = Seq("order_paid"))
  val billingService = Service("billing")
  val bamService = Service("BAM", subscribes = Seq("*"))

  val repo = new ServicesRepository(ordersService, emailService, billingService, bamService)
  implicit val context = new HubContext(repo)

  "getSubscribers" should "return all subscribers" in {
    val msg = Message("order_paid")
    repo.getSubscribersFor(msg) should be (Seq(ordersService, emailService, bamService))
  }

  "getPublisher" should "return message publishing service" in {
    val msg = Message("order_created")
    repo.getPublisherOf(msg) should be (Some(ordersService))
  }

  "constructor" should "detect duplicate publishers of same message" in {
    val duplicatePublisher = Service("some_other_service", publishes = Seq("order_created"))
    val ex = intercept[NoAuthorityForMessageException] {
      new ServicesRepository(ordersService, duplicatePublisher)
    }
    ex.getMessage should include ("orders, some_other_service")
    ex.getMessage should include ("order_created")
  }
}

class IntegrationTest extends SpecBase {
  "message" should "have the implicit context" in {
    implicit val ctx = new HubContext(new ServicesRepository())
    val msg = Message("done")
    msg.context shouldNot be (null)
  }

//  val repository = new ServicesRepository(
//    Service("orders", publishes = Seq("order_created"), subscribes = Seq("order_paid"), endpoints = Seq(Endpoint("http://localhost:8081")))
//  )
//  "Hub" should "delivery message to service" in {
//    new Hub()
//  }
}
