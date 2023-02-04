package ru.auto.salesman.api

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader

object SalesmanApiUtils {

  implicit class SalesmanHttpRequest(val request: HttpRequest) extends AnyVal {

    def withSalesmanTestHeader(): HttpRequest =
      request.withHeaders(RawHeader("X-Salesman-User", "test"))
  }
}
