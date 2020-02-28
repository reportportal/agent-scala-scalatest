name := "agent-scala-scalatest"

organization := "com.epam.reportportal"

scalaVersion := "2.12.10"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.10")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

licenses += "Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0")

resolvers ++= Seq(
  Resolver.sbtPluginRepo("releases"),
  "jitpack" at "https://jitpack.io",
  "EPAM bintray" at "https://dl.bintray.com/epam/reportportal"
)

libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "3.0",
  "com.google.guava" % "guava" % "22.0",
  "com.github.reportportal" % "client-java" % "dc4cb35",
  "com.github.reportportal" % "commons-model" % "37e96a6",
  "org.scalatest" %% "scalatest" % "3.0.8",
  "com.epam.reportportal" % "logger-java-logback" % "5.0.0-BETA-8" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
  "ch.qos.logback" % "logback-core" % "1.2.3" % "test"
)

releaseCrossBuild := true

bintrayOrganization := Some("epam")

bintrayRepository := "reportportal"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-C", "com.epam.reportportal.scalatest.RPReporter", "-P1")

parallelExecution in Test := false

logBuffered in Test := false
