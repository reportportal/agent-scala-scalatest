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

package com.epam.reportportal.scalatest

import org.scalatest.{FunSuite, Matchers}

class RPReporterTest extends FunSuite with Matchers {

  test("Checking System properties are set by RPReporter") {
    val reporter = new RPReporter()
    System.getProperty("rp.endpoint") shouldBe("http://192.168.0.105:8080")
    System.getProperty("rp.uuid") shouldBe("aaaaaaaa-bbbb-cccc-dddd-000000000000")
    System.getProperty("rp.launch") shouldBe("superadmin_TEST_EXAMPLE")
    System.getProperty("rp.project") shouldBe("default_project")
    System.getProperty("rp.enable") shouldBe("true")
    System.getProperty("rp.tags") shouldBe("Unittests")
    reporter.init()
    reporter.reporterService should not be(null)
  }
}
