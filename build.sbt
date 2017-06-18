name := "money-transfer"

version := "1.0"

scalaVersion := "2.12.1"

enablePlugins(JavaAppPackaging)

libraryDependencies ++= {
  val akkaV = "2.4.18"
  val akkaHttpV = "10.0.7"
  val jacksonV = "2.8.7"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonV,
    "com.typesafe.akka" %%  "akka-testkit" % akkaV % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % "test",
    "org.scalatest" %% "scalatest" % "3.0.3" % "test"
  )
}

mainClass in Compile := Some("antdek.moneytransfer.Main")