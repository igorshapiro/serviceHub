import scala.reflect.io.{File, Directory}

object Boot extends App {
  println(Directory.Current)
  println(File(".").toAbsolute)
  println(File("./test-assets/manifest-loading-test/services.json").bufferedReader().readLine())
}
