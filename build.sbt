name := "hub"

version := "1.0"

scalaVersion := "2.11.5"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "The New Motion Public Repo" at "http://nexus.thenewmotion.com/content/groups/public/"

libraryDependencies ++= {
  val sprayV = "1.3.2"
  val akkaV = "2.3.9"
  Seq(
    "org.scala-lang" % "scala-reflect" % "2.11.5",
    "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %%  "spray-client" % sprayV,
    "io.spray" %%  "spray-routing" % sprayV,
    "io.spray" %%  "spray-json" % "1.3.1",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,
    "org.json4s" %% "json4s-native" % "3.2.10",
    "com.github.nscala-time" %% "nscala-time" % "1.8.0",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
    "com.thenewmotion.akka" %% "akka-rabbitmq" % "1.2.4",
    "org.mongodb" %% "casbah" % "2.8.0"
  )
}
