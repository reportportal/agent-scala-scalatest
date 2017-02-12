name := "agent-scala-scalatest"

organization := "com.epam.reportportal"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

licenses += "GPL-3.0" -> url("https://www.gnu.org/licenses/gpl-3.0.html")

resolvers ++= Seq(
  "EPAM bintray" at "http://dl.bintray.com/epam/reportportal"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1",
  "com.epam.reportportal" % "client-java-core" % "2.6.0"
)

releaseCrossBuild := true

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest)

parallelExecution in Test := false

resolvers += Resolver.jcenterRepo

bintrayOrganization := Some("epam")

bintrayRepository := "reportportal"
