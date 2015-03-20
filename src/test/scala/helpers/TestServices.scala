package helpers

import org.serviceHub.domain.{Message, Service}

trait TestServices {
  val ordersService = Service("orders", publishes = Seq("order_created"), subscribes = Seq("order_paid"))
  val billingService = Service("billing", publishes = Seq("order_paid"), subscribes = Seq("order_created"))
  val bamService = Service("bam", subscribes = Seq("*"))

  val orderCreatedMsg = Message("order_created")
}
