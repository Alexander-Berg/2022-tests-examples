package ru.auto.salesman.api.v1

import akka.http.scaladsl.model.headers.RawHeader
import ru.auto.salesman.util.OperatorContext

/** Contains useful headers for testing purposes
  */
trait RequiredHeaders {

  val operatorUid = "10"
  val operatorContext = OperatorContext("foo", operatorUid)

  val oc = operatorContext

  val RequestIdentityHeaders = List(
    RawHeader("X-Salesman-User", operatorUid.toString),
    RawHeader("X-Yandex-Request-ID", operatorContext.id.toString)
  )
}

object RequiredHeaders extends RequiredHeaders
