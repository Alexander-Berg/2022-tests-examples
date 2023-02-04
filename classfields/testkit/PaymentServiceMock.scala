package ru.auto.comeback.services.testkit

import ru.auto.comeback.services.PaymentsService
import ru.auto.comeback.services.PaymentsService.PaymentsService
import zio.test.mock
import zio.{Has, Task, URLayer, ZLayer}
import zio.test.mock.Mock

object PaymentServiceMock extends Mock[PaymentsService] {

  object HasRegularPayments extends Effect[Long, Throwable, Boolean]

  override val compose: URLayer[Has[mock.Proxy], PaymentsService] = ZLayer.fromService { proxy =>
    new PaymentsService.Service {
      override def hasRegularPayments(clientId: Long): Task[Boolean] = proxy(HasRegularPayments, clientId)

      override def requireRegularPayments[R](clientId: Long)(onSuccess: => Task[R], onFail: => Task[R]): Task[R] =
        hasRegularPayments(clientId).flatMap {
          case true => onSuccess
          case false => onFail
        }
    }
  }
}
