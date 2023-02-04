package ru.auto.salesman.api.v1

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader

object SalesmanApiUtils {

  implicit class SalesmanHttpRequest(val request: HttpRequest) extends AnyVal {

    def withSalesmanTestHeader(): HttpRequest =
      request.withHeaders(RawHeader("X-Salesman-User", "test"))
  }
}
