package ru.yandex.realty

import _root_.akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import ru.yandex.realty.api.{ExceptionsHandler, RejectionsHandler}
import ru.yandex.realty.application.BnbSearcherBuilder

class BnbSearcherComponent extends BnbSearcherBuilder(BnbSearcherConfigBuilder.config) {

  lazy val route: Route = apiHandler.route

  lazy val exceptionHandler: ExceptionHandler = ExceptionsHandler.handler

  lazy val rejectionHandler: RejectionHandler = RejectionsHandler.handler

}
