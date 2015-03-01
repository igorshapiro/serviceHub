name := "hub"

version := "1.0"

scalaVersion := "2.11.5"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "The New Motion Public Repo" at "http://nexus.thenewmotion.com/content/groups/public/"

libraryDependencies ++= {
  val sprayV = "1.3.2"
  Seq(
    "org.scala-lang" % "scala-reflect" % "2.11.5",
    "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %%  "spray-routing" % sprayV,
    "io.spray" %%  "spray-json" % "1.3.1",
    "org.json4s" %% "json4s-native" % "3.2.10"
  )
}
