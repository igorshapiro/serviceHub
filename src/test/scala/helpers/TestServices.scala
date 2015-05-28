package helpers

import org.serviceHub.domain.{Endpoint, Message, Service}
import org.serviceHub.utils.SecureRandom

trait TestServices {
  val rabbitMQTestUrl = "rabbitmq://127.0.0.1/test"
  private val testId = SecureRandom.newId()
  val ordersService = Service(s"orders_$testId",
    publishes = Seq("order_created"),
    subscribes = Seq("order_paid"),
    endpoints = Seq(Endpoint("http://localhost:8081/orders/:type")),
    queue = rabbitMQTestUrl
  )
  val billingService = Service(s"billing_$testId",
    publishes = Seq("order_paid"),
    subscribes = Seq("order_created"),
    endpoints = Seq(Endpoint("http://localhost:8081/billing/:type")),
    queue = rabbitMQTestUrl
  )
  val bamService = Service(s"bam_$testId",
    subscribes = Seq("*"),
    endpoints = Seq(Endpoint("http://localhost:8081/bam/:env/:type")),
    queue = rabbitMQTestUrl
  )

  val orderCreatedMsg = Message("order_created")
}
