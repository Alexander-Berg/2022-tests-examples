package ru.yandex.realty.http

import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.request.{Request, TestRequest}
import ru.yandex.realty.tracing.Traced

/**
  * Introduces implicit [[Request]] into scope.
  * Suppose to be convenient for testing purposes.
  *
  * @author dimas
  */
trait RequestAware {

  implicit val emptyTraced: Traced = Traced.empty

  def withRequestContext[A](user: UserRef)(f: Request => A): A = {
    f(TestRequest.fromUser(user)) // TODO use generators
  }

}
