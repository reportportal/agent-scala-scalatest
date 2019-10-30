## agent-scala-scalatest
###Project for Scalatest agent

This Project is a Report Portal Agent for scalatest projects.

####To use the agent:
* add this library to your project as dependency
  * SBT
   >      resolvers ++= Seq(
   >        "EPAM bintray" at "http://dl.bintray.com/epam/reportportal"
   >      )
   >
   >      libraryDependencies += "com.epam.reportportal" %% "agent-scala-scalatest" % "2.6.0" % "test"
   >

  * Maven
  
   >      <repositories>
   >           <repository>
   >              <snapshots>
   >                <enabled>false</enabled>
   >              </snapshots>
   >              <id>bintray-epam-reportportal</id>
   >              <name>bintray</name>
   >              <url>http://dl.bintray.com/epam/reportportal</url>
   >           </repository>
   >      </repositories>
   >      
   >      <dependency>
   >        <groupId>com.epam.reportportal</groupId>
   >        <artifactId>agent-scala-scalatest_2.11</artifactId>
   >        <version>2.6.0</version>
   >        <scope>test</scope>
   >      </dependency>

   * Gradle
   >        repositories {
   >            jcenter()
   >            mavenLocal()
   >            maven { url "http://dl.bintray.com/epam/reportportal" }
   >        }
   >
   >      testCompile group: 'com.epam.reportportal', name: 'agent-scala-scalatest_2.11', version: '2.6.0'
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
  

