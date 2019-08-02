/*
 * Copyright 2016 EPAM Systems
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-scala-scalatest
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.epam.reportportal.scalatest.service

import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

import com.epam.reportportal.listeners.{ListenerParameters, Statuses}
import com.epam.reportportal.scalatest.domain.TestContext
import com.epam.reportportal.service.{Launch, ReportPortal}
import com.epam.ta.reportportal.ws.model.issue.Issue
import com.epam.ta.reportportal.ws.model.launch.Mode
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ
import com.epam.ta.reportportal.ws.model.{FinishExecutionRQ, FinishTestItemRQ, StartTestItemRQ}
import io.reactivex.Maybe
import org.scalatest.events._
import org.slf4j.{Logger, LoggerFactory}

/*
 * Implements communication with the ReportPortal.
 */
class ReporterServiceImp(parameters: ListenerParameters, launch: Launch, testContext: TestContext) extends ReporterService {

  private val logger = LoggerFactory.getLogger(classOf[ReporterServiceImp])

  private var launchRunningMode: Mode = _
  private var description: String = _
  private var isSkippedAnIssue: Boolean = _

  init()

  def init(): Unit = {
    description = parameters.getDescription
    launchRunningMode = parameters.getLaunchRunningMode
    isSkippedAnIssue = parameters.getSkippedAnIssue
  }

  def startLaunch(event: RunStarting): Unit = {

    try {
      //var rs: EntryCreatedRS = null
      val rs = launch.start()
      testContext.launchID = rs;
    }
    catch {
      case e: Exception => {
        handleException(e, logger, "Unable start the launch: '" + testContext.launchName + "'")
      }
    }
  }

  def finishLaunch(event: RunCompleted): Unit = {
    val rq: FinishExecutionRQ = new FinishExecutionRQ
    rq.setEndTime(Calendar.getInstance.getTime)
    val status = testContext.isLaunchFailed match {
      case true => Statuses.FAILED
      case _ => Statuses.PASSED
    }
    rq.setStatus(status)
    try
      launch.finish(rq)
    catch {
      case e: Exception => {
        handleException(e, logger, "Unable finish the launch: '" + testContext.launchID + "'")
      }
    }
  }

  def startTestSuite(event: SuiteStarting): Unit = {
    val rq: StartTestItemRQ = new StartTestItemRQ {
      setName(event.suiteName)
      setStartTime(Calendar.getInstance.getTime)
      setType("SUITE")
    }
    try {
      val rs = launch.startTestItem(rq)
      testContext.rootIdsOfSuites.put(event.suiteId, rs)
      testContext.suitPassed.put(event.suiteId, true)
    }
    catch {
      case e: Exception => {
        handleException(e, logger, "Unable start test suite: '" + event.suiteName + "'")
      }
    }
  }

  def finishTestSuite(e: SuiteCompleted): Unit = {
    val rq = new FinishTestItemRQ
    rq.setEndTime(Calendar.getInstance.getTime)
    val status = if (testContext.suitPassed.get(e.suiteId)) "PASSED" else "FAILED"
    rq.setStatus(status)
    getValueOfMap(testContext.rootIdsOfSuites, e.suiteId) match {
      case Some(id) => {
        Some(rq.setStatus(status))
        try {
          launch.finishTestItem(id, rq)
        } catch {
          case ex: Exception => handleException(ex, logger, "Unable finish test suite: '" + e.suiteId + "'")
        }
      }
      case None => {
        handleException(new RuntimeException("Missing SuitId to finish"), logger, "Unable finish test suite: '" + e.suiteName + "'")
      }
    }
  }

  def startTestClass(event: SuiteStarting): Unit = {
    val rq = new StartTestItemRQ {
      setName(event.suiteName)
      setDescription(event.suiteId)
      setStartTime(Calendar.getInstance.getTime)
      setType("TEST")
    }
    try {
      val rs = (launch.startTestItem(rq))
      testContext.rootIdsOfSuites.put(event.suiteId, rs)
      testContext.suitPassed.put(event.suiteId, true)
    }
    catch {
      case e: Exception => {
        handleException(e, logger, new StringBuilder("Unable start test method: '").append(event.suiteId).append("'").toString)
      }
    }
  }

  def finishTestClass(event: SuiteCompleted): Unit = {
    val rq = new FinishTestItemRQ
    rq.setEndTime(Calendar.getInstance.getTime)
    getValueOfMap(testContext.rootIdsOfSuites, event.suiteId) match {
      case Some(value) => {
        try
          launch.finishTestItem(value, rq)
        catch {
          case e: Exception => {
            handleException(e, logger, "Unable finish test: '" + value + "'")
          }
        }
      }
      case None => handleException(new RuntimeException("Missing testId."), logger, "Unable finish test: '" + event.suiteId + "'")
    }
  }

  def handleException(exception: Exception, logger: Logger, str: String): Unit = {
    exception.printStackTrace();
    logger.error(str);
  }

  def finishTestClass(event: SuiteAborted): Unit = {
    val rq = new FinishTestItemRQ {
      setEndTime(Calendar.getInstance.getTime)
      setStatus("FAILED")
    }
    event.throwable match {
      case Some(ex) => {
        val issue = new Issue
        issue.setComment(event.message + ex.getMessage)
        issue.setIssueType("Exception")
        rq.setIssue(issue)
      }
      case None => {
        val issue = new Issue
        issue.setComment(s"Suite is aborted abnormally. ${event.message}")
        issue.setIssueType("SuiteAborted")
        rq.setIssue(issue)
      }
    }
    getValueOfMap(testContext.rootIdsOfSuites, event.suiteId) match {
      case Some(value) => {
        try
          launch.finishTestItem(value, rq)
        catch {
          case e: Exception => {
            handleException(e, logger, "Unable finish test: '" + value + "'")
          }
        }
      }
      case None => handleException(new RuntimeException("Missing testId."), logger, "Unable finish test: '" + event.suiteId + "'")
    }
  }

  def startTestMethod(event: TestStarting) {
    val rq = new StartTestItemRQ() {
      setName(event.testName)
      setDescription(createStepDescription(event))
      setStartTime(Calendar.getInstance.getTime)
      setType("STEP")
    }
    var rs: Maybe[String] = null
    try
      getValueOfMap(testContext.rootIdsOfSuites, event.suiteId) match {
        case Some(rsId) => {
          rs = launch.startTestItem(rsId, rq)
          testContext.rootIdsOfSuites.put(event.testName, rs)
        }
        case None => handleException(new RuntimeException(s"Unable start test method: ${event.testText}"),
          logger, new StringBuilder("Unable start test method: '").append(event.testText).append("'").toString)
      }
    catch {
      case e: Exception => {
        handleException(e, logger, new StringBuilder("Unable start test method: '").append(event.testText).append("'").toString)
      }
    }
  }

  def startTestMethod(event: TestIgnored) {
    val rq = new StartTestItemRQ() {
      setName(event.testName)
      setDescription(createStepDescription(event))
      setStartTime(Calendar.getInstance.getTime)
      setType("STEP")
    }
    var rs: Maybe[String] = null
    try
      getValueOfMap(testContext.rootIdsOfSuites, event.suiteId) match {
        case Some(rsId) => {
          rs = launch.startTestItem(rsId, rq)
          testContext.rootIdsOfSuites.put(event.testName, rs)
        }
        case None => handleException(new RuntimeException(s"Unable start test method: ${event.testText}"),
          logger, new StringBuilder("Unable start test method: '").append(event.testText).append("'").toString)
      }
    catch {
      case e: Exception => {
        handleException(e, logger, new StringBuilder("Unable start test method: '").append(event.testText).append("'").toString)
      }
    }
  }

  def startTestMethod(event: TestPending) {
    val rq = new StartTestItemRQ() {
      setName(event.testName)
      setDescription(createStepDescription(event))
      setStartTime(Calendar.getInstance.getTime)
      setType("STEP")
    }
    var rs: Maybe[String] = null
    try
      getValueOfMap(testContext.rootIdsOfSuites, event.suiteId) match {
        case Some(rsId) => {
          rs = launch.startTestItem(rsId, rq)
          testContext.rootIdsOfSuites.put(event.testName, rs)
        }
        case None => handleException(new RuntimeException(s"Unable start test method: ${event.testText}"),
          logger, new StringBuilder("Unable start test method: '").append(event.testText).append("'").toString)
      }
    catch {
      case e: Exception => {
        handleException(e, logger, new StringBuilder("Unable start test method: '").append(event.testText).append("'").toString)
      }
    }
  }

  def finishTestMethod(event: Any): Unit = {
    val rq = new FinishTestItemRQ
    rq.setEndTime(Calendar.getInstance.getTime)
    val (status, testId) = event match {
      case e: TestSucceeded => (Statuses.PASSED, e.testName)
      case e: TestFailed => {
        testContext.isLaunchFailed = true
        testContext.suitPassed.put(e.suiteId, false)
        (Statuses.FAILED, e.testName)
      }
      case e: TestIgnored => {
        val issue = new Issue {
          setIssueType("NOT_ISSUE")
        }
        rq.setIssue(issue)
        (Statuses.SKIPPED, e.testName)
      }
      case e: TestPending => {
        testContext.isLaunchFailed = true
        testContext.suitPassed.put(e.suiteId, false)
        (Statuses.SKIPPED, e.testName)
      }
      case e: TestCanceled => {
        testContext.isLaunchFailed = true
        testContext.suitPassed.put(e.suiteId, false)
        (Statuses.SKIPPED, e.testName)
      }
      case _ => (Statuses.SKIPPED, "NO_ID")
    }
    rq.setStatus(status)
    getValueOfMap(testContext.rootIdsOfSuites, testId) match {
      case Some(value) => {
        try {
          launch.finishTestItem(value, rq)
        }
        catch {
          case e: Exception => {
            handleException(e, logger, "Unable finish test: '" + value + "'")
          }
        }
      }
      case None => handleException(new RuntimeException("Missing test ID."), logger, "Unable finish test in '" + testId + "'")
    }
  }

  def startConfiguration(e: DiscoveryStarting): Unit = {

  }

  def sendReportPortalMsg(e: TestFailed): Unit = {

    try {

      val value: rp.com.google.common.base.Function[String, SaveLogRQ] = new rp.com.google.common.base.Function[String, SaveLogRQ] {
        override def apply(itemId: String): SaveLogRQ = {
          val saveLogRequest = new SaveLogRQ
          saveLogRequest.setItemId(itemId)
          saveLogRequest.setLevel("ERROR")
          saveLogRequest.setLogTime(Calendar.getInstance.getTime)
          saveLogRequest.setMessage(e.message)
          saveLogRequest.setLogTime(Calendar.getInstance.getTime)
          saveLogRequest
        }
      }

      ReportPortal.emitLog(value);
    }
    catch {
      case ex: Exception => {
        handleException(ex, logger, "Unable to send message to Report Portal")
      }
    }
  }

  private def getValueOfMap(map: ConcurrentHashMap[String, Maybe[String]], key: String): Option[Maybe[String]] = {
    map.containsKey(key) match {
      case true => Some(map.get(key))
      case _ => None
    }
  }

  private def createStepDescription(event: TestStarting) = {
    val stringBuffer = new StringBuilder
    if (event.testName != null) stringBuffer.append(event.testName)
    stringBuffer.toString
  }

  private def createStepDescription(event: TestIgnored) = {
    val stringBuffer = new StringBuilder
    if (event.testName != null) stringBuffer.append(event.testName)
    stringBuffer.toString
  }

  private def createStepDescription(event: TestPending) = {
    val stringBuffer = new StringBuilder
    if (event.testName != null) stringBuffer.append(event.testName)
    stringBuffer.toString
  }
}
