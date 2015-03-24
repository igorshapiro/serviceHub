import helpers.SpecBase
import org.serviceHub.utils.SecureRandom

class SecureRandomTest extends SpecBase {
  "newId" should "generate random tokens" in {
    val ITERATIONS = 100
    val ids = (for (x <- 1 to ITERATIONS) yield SecureRandom.newId(bytes = 4)).toSet

    ids.size should be (100)
  }

  "newId" should "prefix the token with <prefix> parameter" in {
    val id = SecureRandom.newId(prefix = "ent")
    id should startWith ("ent_")
  }

  "newId" should "not add underscore if there's no prefix" in {
    SecureRandom.newId(prefix = "") shouldNot contain("_")
  }
}
