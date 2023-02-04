package ru.auto.api.managers

import ru.auto.api.auth.Application
import ru.auto.api.model.{RequestParams, UserRef}
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.tracing.Traced

trait TestRequestWithId {

  val testRequestId = "test-request"

  implicit def request: Request = {
    val r = new RequestImpl {
      override def requestId: String = testRequestId
    }
    r.setTrace(Traced.empty)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.swagger)
    r.setUser(UserRef.user(1))
    r
  }
}
