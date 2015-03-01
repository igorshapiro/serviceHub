package helpers

import java.nio.file.Paths

class Scenario(name: String) {
  def absoluteDir = Paths.get(System.getProperty("user.dir"), "scenarios", name).toAbsolutePath.toString
}

object Scenario {
  def apply(name: String) = new Scenario(name)
}