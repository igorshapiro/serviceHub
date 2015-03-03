package helpers

import org.scalamock.scalatest.MockFactory
import org.scalatest._

abstract class SpecBase extends FlatSpec with Matchers with MockFactory with BeforeAndAfterAll {

}
