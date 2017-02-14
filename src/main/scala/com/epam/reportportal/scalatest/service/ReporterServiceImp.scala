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

import com.epam.reportportal.listeners.ListenersUtils.handleException
import com.epam.reportportal.listeners.{ListenerParameters, Statuses}
import com.epam.reportportal.scalatest.domain.TestContext
import com.epam.reportportal.service.BatchedReportPortalService
import com.epam.ta.reportportal.ws.model.issue.Issue
import com.epam.ta.reportportal.ws.model.launch.{Mode, StartLaunchRQ}
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ
import com.epam.ta.reportportal.ws.model.{EntryCreatedRS, FinishExecutionRQ, FinishTestItemRQ, StartTestItemRQ}
import com.google.inject.Inject
import org.scalatest.events._
import org.slf4j.LoggerFactory

/*
 * Implements communication with the ReportPortal.
 */
class ReporterServiceImp @Inject()(parameters: ListenerParameters, service: BatchedReportPortalService, testContext: TestContext) extends ReporterService {

  private val logger = LoggerFactory.getLogger(classOf[ReporterServiceImp])

  private var launchRunningMode: Mode = _
  private var description: String = _
  private var isSkippedAnIssue: Boolean = _

  init()

  def init(): Unit = {
    description = parameters.getDescription
    launchRunningMode = parameters.getMode
    isSkippedAnIssue = parameters.getIsSkippedAnIssue
  }

  def startLaunch(event: RunStarting): Unit = {
    val rq = new StartLaunchRQ {
      setName(parameters.getLaunchName)
      setStartTime(Calendar.getInstance.getTime)
      setTags(parameters.getTags)
      setMode(parameters.getMode)
    }
    if (description != null) rq.setDescription(description)

    try {
      //var rs: EntryCreatedRS = null
      val rs = service.startLaunch(rq)
      testContext.launchID = rs.getId
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
      service.finishLaunch(testContext.launchID, rq)
    catch {
      case e: Exception => {
        handleException(e, logger, "Unable finish the launch: '" + testContext.launchID + "'")
      }
    }
  }

  def startTestSuite(event: SuiteStarting): Unit = {
    val rq: StartTestItemRQ = new StartTestItemRQ {
      setLaunchId(testContext.launchID)
      setName(event.suiteName)
      setStartTime(Calendar.getInstance.getTime)
      setType("SUITE")
    }
    try {
      val rs = service.startRootTestItem(rq)
      testContext.rootIdsOfSuites.put(event.suiteId, rs.getId)
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
        Some(rq.setStatus(id))
        try {
          service.finishTestItem(id, rq)
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
      setLaunchId(testContext.launchID)
      setDescription(event.suiteId)
      setStartTime(Calendar.getInstance.getTime)
      setType("TEST")
    }
    try {
      val rs = (service.startRootTestItem(rq))
      testContext.rootIdsOfSuites.put(event.suiteId, rs.getId)
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
          service.finishTestItem(value, rq)
        catch {
          case e: Exception => {
            handleException(e, logger, "Unable finish test: '" + value + "'")
          }
        }
      }
      case None => handleException(new RuntimeException("Missing testId."), logger, "Unable finish test: '" + event.suiteId + "'")
    }
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
          service.finishTestItem(value, rq)
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
      setLaunchId(testContext.launchID)
      setDescription(createStepDescription(event))
      setStartTime(Calendar.getInstance.getTime)
      setType("STEP")
    }
    var rs: EntryCreatedRS = null
    try
      getValueOfMap(testContext.rootIdsOfSuites, event.suiteId) match {
        case Some(rsId) => {
          rs = service.startTestItem(rsId, rq)
          testContext.rootIdsOfSuites.put(event.testName, rs.getId)
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
      setLaunchId(testContext.launchID)
      setDescription(createStepDescription(event))
      setStartTime(Calendar.getInstance.getTime)
      setType("STEP")
    }
    var rs: EntryCreatedRS = null
    try
      getValueOfMap(testContext.rootIdsOfSuites, event.suiteId) match {
        case Some(rsId) => {
          rs = service.startTestItem(rsId, rq)
          testContext.rootIdsOfSuites.put(event.testName, rs.getId)
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
      setLaunchId(testContext.launchID)
      setDescription(createStepDescription(event))
      setStartTime(Calendar.getInstance.getTime)
      setType("STEP")
    }
    var rs: EntryCreatedRS = null
    try
      getValueOfMap(testContext.rootIdsOfSuites, event.suiteId) match {
        case Some(rsId) => {
          rs = service.startTestItem(rsId, rq)
          testContext.rootIdsOfSuites.put(event.testName, rs.getId)
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
          service.finishTestItem(value, rq)
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
    val saveLogRequest = new SaveLogRQ
    saveLogRequest.setTestItemId(testContext.rootIdsOfSuites.get(e.testName))
    saveLogRequest.setLevel("ERROR")
    saveLogRequest.setLogTime(Calendar.getInstance.getTime)
    saveLogRequest.setMessage(e.message)
    saveLogRequest.setLogTime(Calendar.getInstance.getTime)
    try {
      service.log(saveLogRequest)
    }
    catch {
      case ex: Exception => {
        handleException(ex, logger, "Unable to send message to Report Portal")
      }
    }
  }

  private def getValueOfMap(map: ConcurrentHashMap[String, String], key: String): Option[String] = {
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
