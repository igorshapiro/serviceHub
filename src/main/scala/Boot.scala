import akka.actor.ActorSystem
import org.serviceHub.ServiceHub
import org.serviceHub.domain.ServicesRepository

object Boot extends App {
  implicit val system = ActorSystem()
  val repo = new ServicesRepository(".")
  val hub = new ServiceHub(repo.services:_*)
}
