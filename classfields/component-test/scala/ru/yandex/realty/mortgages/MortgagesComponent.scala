package ru.yandex.realty.mortgages

import _root_.akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import ru.yandex.realty.mortgages.api.{ExceptionsHandler, RejectionsHandler}
import ru.yandex.realty.mortgages.application.MortgagesApp

class MortgagesComponent extends MortgagesApp(MortgagesConfigBuilder.config) {

  lazy val route: Route = apiHandler.route

  lazy val exceptionHandler: ExceptionHandler = ExceptionsHandler.handler

  lazy val rejectionHandler: RejectionHandler = RejectionsHandler.handler

}
