package helpers

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps

abstract class ActorSpecBase extends SpecBase with TestKitBase {
  implicit lazy val system: ActorSystem = ActorSystem()
  implicit val timeout = Timeout(5 seconds)

  override protected def afterAll(): Unit = shutdown(system)
}
