package helpers

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import akka.util.Timeout
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

abstract class SpecBase extends FlatSpec
  with Matchers
  with MockFactory
  with ScalaFutures
  with BeforeAndAfterEach
  with BeforeAndAfterAll {
  protected implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(150, Millis))
}

abstract class ActorSpecBase extends SpecBase with TestKitBase {
  implicit lazy val system: ActorSystem = ActorSystem()
  implicit val timeout = Timeout(5 seconds)

  override protected def afterAll(): Unit = shutdown(system)
}