/*
 * Copyright 2016 EPAM Systems
 *
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

package com.epam.reportportal.scalatest

import java.io.InputStream
import java.util.Properties

import org.scalatest.Reporter
import org.scalatest.events._
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.epam.reportportal.scalatest.providers.TestContextProvider
import com.epam.reportportal.scalatest.service.ReporterServiceImp
import com.google.inject.Guice
import org.slf4j.LoggerFactory

/**
  * Own Reporter implementation to send test information to ReportPortal server.
  */
class RPReporter extends Reporter {

  private val reportPortalPropertiesFileName = "reportportal.properties"
  private[scalatest] var reporterService: Supplier[ReporterServiceImp] = _
  private var isSuiteStarted: ThreadLocal[Boolean] = _
  private val logger = LoggerFactory.getLogger(classOf[RPReporter])

  init()

  private[scalatest] def init(): Unit = {
    loadReportPortalProperties()
    isSuiteStarted = new ThreadLocal[Boolean]
    isSuiteStarted.set(false)
    reporterService = Suppliers.memoize(new Supplier[ReporterServiceImp] {
      override def get() = Guice.createInjector(new TestContextProvider).getInstance(classOf[ReporterServiceImp])
    })
  }

  /*
   * The reportportal.properties is not loaded by client-java-core using sbt goal, we load it into System properties here.
   * It is a workaround.
   */
  private[scalatest] def loadReportPortalProperties(): Unit = {
    try {
      val properties: Properties = new Properties()
      val inputStream: InputStream = getClass().getClassLoader().getResourceAsStream(reportPortalPropertiesFileName)
      properties.load(inputStream)
      properties.stringPropertyNames().toArray().foreach(p =>
        System.setProperty(p.toString, properties.getProperty(p.toString)))
    } catch {
      case e: Exception => logger.warn(s"$reportPortalPropertiesFileName file is not found", e)
    }
  }

  def apply(event: Event): Unit = event match {

    case e: RunStarting ⇒ {

      reporterService.get().startLaunch(e)
    }

    case e: RunCompleted ⇒ {
      reporterService.get().finishLaunch(e)
    }

    case e: SuiteStarting ⇒ {
      val klass = Class.forName(e.suiteClassName.get)
      // we don't report yet suits that has nested suites
      if (Class.forName("org.scalatest.Suites").isAssignableFrom(klass)) {
        // will be implemented later
        // Do nothing right now
      } else {
        reporterService.get().startTestClass(e)
      }
    }

    case e: SuiteCompleted ⇒ {
      val klass = Class.forName(e.suiteClassName.get)
      // we don't report yet suits that has nested suites
      if (Class.forName("org.scalatest.Suites").isAssignableFrom(klass)) {
        // will be implemented later
        // Do nothing right now
      } else {
        reporterService.get() finishTestClass (e: SuiteCompleted)
      }
    }

    case (e: SuiteAborted) ⇒ {
      val klass = Class.forName(e.suiteClassName.get)
      // we don't report yet suits that has nested suites
      if (Class.forName("org.scalatest.Suites").isAssignableFrom(klass)) {
        // will be implemented later
        // Do nothing right now
      } else {
        reporterService.get() finishTestClass (e)
      }
    }

    case e: TestStarting ⇒ {
      reporterService.get().startTestMethod(e)
    }

    case e: TestCanceled ⇒ {
      reporterService.get().finishTestMethod(e)
    }

    case e: TestFailed ⇒ {
      reporterService.get().sendReportPortalMsg(e)
      reporterService.get().finishTestMethod(e)
    }

    case e: TestIgnored ⇒ {
      reporterService.get().startTestMethod(e)
      reporterService.get().finishTestMethod(e)
    }

    case e: TestPending ⇒ {
      reporterService.get().finishTestMethod(e)
    }

    case e: TestSucceeded ⇒ {
      reporterService.get().finishTestMethod(e)
    }

    case e: InfoProvided ⇒ {
      println(e.getClass.getSimpleName)
    }

    case e: DiscoveryStarting ⇒ logger.info(e.getClass.getSimpleName)
    case e: AlertProvided ⇒ logger.info(e.getClass.getSimpleName)
    case e: DiscoveryCompleted ⇒ logger.info(e.getClass.getSimpleName)
    case e: MarkupProvided ⇒ logger.info(e.getClass.getSimpleName)
    case e: NoteProvided ⇒ logger.info(e.getClass.getSimpleName)
    case e: RunAborted ⇒ logger.info(e.getClass.getSimpleName)
    case e: RunStopped ⇒ logger.info(e.getClass.getSimpleName)
    case e: ScopeClosed ⇒ logger.info(e.getClass.getSimpleName)
    case e: ScopeOpened ⇒ logger.info(e.getClass.getSimpleName)
    case e: ScopePending ⇒ logger.info(e.getClass.getSimpleName)

  }
}
