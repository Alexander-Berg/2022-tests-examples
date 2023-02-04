package ru.yandex.realty.mortgages

import _root_.akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import ru.yandex.realty.componenttest.env.ComponentTestEnvironmentProvider
import ru.yandex.realty.componenttest.spec.{HttpComponentTestSpec, WireMockComponentTestSpec}

abstract class AbstractMortgagesComponentTestSpec
  extends ComponentTestEnvironmentProvider[MortgagesEnvironment.type]
  with HttpComponentTestSpec[MortgagesEnvironment.type]
  with WireMockComponentTestSpec[MortgagesEnvironment.type] {

  override lazy val env: MortgagesEnvironment.type = MortgagesEnvironment

  protected lazy val component: MortgagesComponent = env.component

  override lazy val routeUnderTest: Route = component.route

  override lazy val exceptionHandler: ExceptionHandler = component.exceptionHandler
  override lazy val rejectionHandler: RejectionHandler = component.rejectionHandler

  env

}
