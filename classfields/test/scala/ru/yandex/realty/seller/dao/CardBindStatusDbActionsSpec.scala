package ru.yandex.realty.seller.dao

import java.sql.BatchUpdateException

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.db.DbSpecBase
import ru.yandex.realty.seller.model.cards.{BindStatusForRequest, BindStatusPatch, BindStatusUpdate, BindStatuses}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.vertis.util.time.DateTimeUtil
import slick.dbio.{DBIOAction, Effect, NoStream}

trait CardBindStatusDbActionsSpec extends AsyncSpecBase with DbSpecBase with SellerModelGenerators {

  implicit class RichDbAction[+R, +S <: NoStream, -E <: Effect](action: DBIOAction[R, S, E]) {
    def execute: R = action.databaseValue.futureValue
  }

  implicit def asSingletonIterable[T](value: T): Iterable[T] = Iterable(value)

  def statusesDb: CardBindStatusDbActions

  "CardBindStatusDbActions" should {

    "correctly add new status" in {
      val status = newCardBindStatusGen.next
      statusesDb.insert(status).execute
    }

    "fail when request exists" in {
      val status = newCardBindStatusGen.next
      statusesDb.insert(status).execute

      interceptCause[BatchUpdateException] {
        statusesDb.insert(status).execute
      }
    }

    "correctly get status" in {
      val status = newCardBindStatusGen.next
      statusesDb.insert(status).execute

      val result = getForRequest(status.uid, status.requestPaymentId)
      result shouldEqual status
    }

    "return None on get non existing" in {
      val filter = BindStatusForRequest(0L, "not_found")

      val result = statusesDb.getStatus(filter).execute
      result should be(None)
    }

    "correctly update status" in {
      val status = newCardBindStatusGen.next
      statusesDb.insert(status).execute

      statusesDb.updateStatus(BindStatusUpdate.status(status, BindStatuses.Cancelled)).execute

      val result = getForRequest(status.uid, status.requestPaymentId)
      result shouldEqual status.copy(status = BindStatuses.Cancelled)
    }

    "correctly update visit time" in {
      val status = newCardBindStatusGen.next

      statusesDb.insert(status).execute

      val date = Some(DateTimeUtil.now().plusHours(1))

      val update = BindStatusUpdate.visitTime(status, visitTime = date)

      statusesDb.updateStatus(update).execute

      getForRequest(status.uid, status.requestPaymentId) shouldEqual update.bindStatusPatch.applyTo(status)

    }

    "correctly update all fields" in {
      val status = newCardBindStatusGen.next.copy(visitTime = None)

      statusesDb.insert(status).execute

      getForRequest(status.uid, status.requestPaymentId).visitTime shouldEqual None

      val date = DateTimeUtil.now()

      val patch = BindStatusPatch(
        BindStatuses.RefundedFailed,
        Some(date)
      )

      val update = BindStatusUpdate(
        status.uid,
        status.requestPaymentId,
        patch
      )

      statusesDb.updateStatus(update).execute

      getForRequest(status.uid, status.requestPaymentId) shouldEqual patch.applyTo(status)
    }
  }

  private def getForRequest(uid: Long, requestPaymentId: String) = {
    val result = statusesDb.getStatus(BindStatusForRequest(uid, requestPaymentId)).execute
    result should not be None
    result.get
  }
}
