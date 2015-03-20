package helpers

import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

abstract class SpecBase extends FlatSpec
  with Matchers
  with MockFactory
  with ScalaFutures
  with BeforeAndAfterEach
  with BeforeAndAfterAll {
  protected implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(150, Millis))
}
