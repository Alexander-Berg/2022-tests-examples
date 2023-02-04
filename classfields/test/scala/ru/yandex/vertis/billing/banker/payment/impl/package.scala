package ru.yandex.vertis.billing.banker.payment

import ru.yandex.vertis.billing.banker.dao.DBIOHelpers.DBIOResult
import ru.yandex.vertis.billing.banker.model.PaymentRequest.EmptyForm
import ru.yandex.vertis.billing.banker.model.{PaymentRequest, PaymentRequestId}
import slick.dbio.DBIO

import scala.concurrent.Future

/**
  * @author alex-kovalenko
  */
package object impl {

  def emptyForm(id: PaymentRequestId): Future[PaymentRequest.Form] =
    Future.successful(EmptyForm(id))

  def emptyAction(): DBIOResult[Unit] =
    DBIO.successful(())
}
