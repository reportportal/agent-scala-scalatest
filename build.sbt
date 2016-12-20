name := "agent-scala-scalatest"

organization := "com.epam.reportportal"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "EPAM bintray" at "http://dl.bintray.com/epam/reportportal"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1",
  "com.epam.reportportal" % "client-java-core" % "2.6.0"
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest)

parallelExecution in Test := false
