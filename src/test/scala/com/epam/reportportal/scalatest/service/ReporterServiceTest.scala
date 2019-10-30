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

import java.util.{Calendar, Date}
import java.util.concurrent.ConcurrentHashMap

import com.epam.reportportal.listeners.ListenerParameters
import com.epam.reportportal.scalatest.domain.TestContext
import com.epam.reportportal.service.{Launch, ReportPortal}
import com.epam.reportportal.utils.properties.PropertiesLoader
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ
import com.google.common.base.{Supplier, Suppliers}
import io.reactivex.Maybe
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import org.slf4j.{Logger, LoggerFactory}


class ReporterServiceTest extends FunSuite with Matchers with BeforeAndAfter{

  private var reporterService: Supplier[ReporterServiceImp] = _
  private val logger: Logger = LoggerFactory.getLogger("test")

  before {
    val propertiesLoader: PropertiesLoader = PropertiesLoader.load
    val listenerParameters: ListenerParameters = new ListenerParameters(propertiesLoader)
    val launch: Launch = Suppliers.memoize(new Supplier[Launch]() {
      override def get: Launch = {
        val reportPortal = ReportPortal.builder.build
        val rq = new StartLaunchRQ {
          setName(listenerParameters.getLaunchName)
          setStartTime(Calendar.getInstance.getTime)
          setAttributes(listenerParameters.getAttributes)
          setMode(listenerParameters.getLaunchRunningMode)
        }
        rq.setStartTime(Calendar.getInstance.getTime)
        val description = listenerParameters.getDescription
        if (description != null) rq.setDescription(description)
        reportPortal.newLaunch(rq)
      }
    }).get()

    val testNGContext: TestContext = TestContext(
      listenerParameters.getLaunchName,
      Maybe.empty(), isLaunchFailed = false, new ConcurrentHashMap[String, Boolean],
      new ConcurrentHashMap[String, Maybe[String]])
    reporterService = Suppliers.memoize(new Supplier[ReporterServiceImp] {
      override def get() = new ReporterServiceImp(listenerParameters, launch, testNGContext)
    })
  }

  test("Testing ReporterService is created ") {
    logger.error("HELLO MESSAGE")
    ReportPortal.emitLog("QWE", "ERROR", new Date());
    reporterService should not be (null)
  }
}
