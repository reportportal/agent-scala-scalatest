name := "agent-scala-scalatest"

organization := "com.epam.reportportal"

scalaVersion := "2.13.1"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.10", "2.13.1")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

licenses += "Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0")

resolvers ++= Seq(
  Resolver.sbtPluginRepo("releases")
)

libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "3.0",
  "com.google.guava" % "guava" % "22.0",
  "com.epam.reportportal" % "client-java" % "5.0.0-BETA-9",
  "com.epam.reportportal" % "commons-model" % "5.0.0",
  "org.scalatest" %% "scalatest" % "3.0.8",
  "com.epam.reportportal" % "logger-java-logback" % "5.0.0-BETA-9" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
  "ch.qos.logback" % "logback-core" % "1.2.3" % "test"
)

releaseCrossBuild := true

bintrayOrganization := Some("epam")

bintrayRepository := "reportportal"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-C", "com.epam.reportportal.scalatest.RPReporter", "-P1")

parallelExecution in Test := false

logBuffered in Test := false
