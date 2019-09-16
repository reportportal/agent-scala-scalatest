name := "agent-scala-scalatest"

organization := "com.epam.reportportal"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

licenses += "GPL-3.0" -> url("https://www.gnu.org/licenses/gpl-3.0.html")

resolvers ++= Seq(
  "EPAM bintray" at "http://dl.bintray.com/epam/reportportal"
)

libraryDependencies ++= Seq("com.google.inject" % "guice" % "3.0",
  "org.scalatest" %% "scalatest" % "3.0.8",
  "com.epam.reportportal" % "client-java" % "5.0.0-BETA-4",
  "com.epam.reportportal" % "commons-model" % "5.0.0-BETA-12",
  "com.epam.reportportal" % "logger-java-logback" % "5.0.0-BETA-4",
//  "org.apache.logging.log4j" % "log4j-api" % "2.8.1",
//  "org.apache.logging.log4j" % "log4j-core" % "2.8.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "com.google.guava" % "guava" % "18.0"
)

releaseCrossBuild := true

resolvers += Resolver.jcenterRepo

bintrayOrganization := Some("epam")

bintrayRepository := "reportportal"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-C", "com.epam.reportportal.scalatest.RPReporter", "-P1")

parallelExecution in Test := false

logBuffered in Test := false