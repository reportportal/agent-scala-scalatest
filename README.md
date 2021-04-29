# agent-scala-scalatest
## Project for Scalatest agent

This Project is a Report Portal Agent for scalatest projects.

### To use the agent:
* add this library to your project as dependency
  * SBT
   >
   >      libraryDependencies += "com.epam.reportportal" %% "agent-scala-scalatest" % "5.0.4" % "test"
   >

  * Maven
  
   >      
   >      <dependency>
   >        <groupId>com.epam.reportportal</groupId>
   >        <artifactId>agent-scala-scalatest_2.12</artifactId>
   >        <version>5.0.4</version>
   >        <scope>test</scope>
   >      </dependency>

   * Gradle
   >        repositories {
   >            mavenCentral()
   >        }
   >
   >      testCompile group: 'com.epam.reportportal', name: 'agent-scala-scalatest_2.12', version: '5.0.4'
   >

* add your reportportal.properties file to test/resources
* set reporter in your build tool:
  * _maven_ (Configuration options / reporters): 
  >`com.epam.reportportal.scalatest.RPReporter`
  > http://www.scalatest.org/user_guide/using_the_scalatest_maven_plugin
  * _sbt:_ 
  > in your build.sbt:
  > ```
  > testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-C", "com.epam.reportportal.scalatest.RPReporter")
  > ```
  * _gradle:_ 
  >in your test task: 
  >```
  > args = ['-C', 'com.epam.reportportal.scalatest.RPReporter']
  >```
  
## Known issues
* Logging into Report Portal does not work for scalatest. Scalatest uses internal 
queue to publish test events, such as "execution start", "test start", "test finish".
Such design causes desync between logging and test events: logging events come
synchronously during test execution and test events come asynchronously after test 
execution. That leads to a situation when a client doesn't know to which test a log
event is related and drop it. 
  * As a possible solution we need to implement a scala log appender, which will
send logging events to the same queue with test events.
