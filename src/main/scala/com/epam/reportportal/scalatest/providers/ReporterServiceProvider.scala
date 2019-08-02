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

package com.epam.reportportal.scalatest.providers

import java.lang.reflect.{InvocationHandler, Method, Proxy}

import com.epam.reportportal.listeners.ListenerParameters
import com.epam.reportportal.scalatest.domain.TestContext
import com.epam.reportportal.scalatest.service.{ReporterService, ReporterServiceImp}
import com.epam.reportportal.service.Launch
import com.google.inject.{Inject, Provider, Provides}

class ReporterServiceProvider @Inject()(listenerParameters: ListenerParameters, testContext: TestContext, reportPortalService: Launch) extends Provider[ReporterService] {

  @Provides
  override def get(): ReporterService = {
    if (listenerParameters.getEnable) return new ReporterServiceImp(listenerParameters, reportPortalService, testContext)

    return Proxy.newProxyInstance(this.getClass.getClassLoader,
      Array[Class[_]](classOf[ReporterService]),
      new InvocationHandler() {
        @throws[Throwable]
        @Override
        def invoke(proxy: Any, method: Method, args: Array[AnyRef]): AnyRef = null
      }).asInstanceOf[ReporterService]
  }
}
