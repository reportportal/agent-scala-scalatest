## agent-scala-scalatest
###Project for Scalatest agent

This Project is a Report Portal Agent for scalatest projects.

####To use the agent:
* add this library to your project as dependency
  * SBT
   >
   >      libraryDependencies += "com.epam.reportportal" %% "agent-scala-scalatest" % "2.6.0" % "test"
   >

  * Maven
   >      <dependency>
   >        <groupId>com.epam.reportportal</groupId>
   >        <artifactId>agent-scala-scalatest_2.11</artifactId>
   >        <version>2.6.0</version>
   >        <scope>test</scope>
   >      </dependency>

   * Gradle
   >
   >      testCompile group: 'com.epam.reportportal', name: 'agent-scala-scalatest_2.11', version: '2.6.0'
   >

* add your reportportal.properties file to test/resources
* set reporter in your build tool:
  * _maven_ (Configuration options / Reporters): 
  
  http://www.scalatest.org/user_guide/using_the_scalatest_maven_plugin
  * _sbt_
  
   In your build.sbt
  ```scala
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-C", "com.epam.reportportal.scalatest.RPReporter")
  ```
  * _gradle:_ in your test task: 
  ```groovy
  args = ['-C', 'com.epam.reportportal.scalatest.RPReporter']
  ```
  

