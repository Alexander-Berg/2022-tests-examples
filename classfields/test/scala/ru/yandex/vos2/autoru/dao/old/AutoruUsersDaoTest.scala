package ru.yandex.vos2.autoru.dao.old

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.CommonModel
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.RecallReason
import ru.yandex.vos2.autoru.utils.RecallUtils

/**
  * Created by andrey on 11/16/16.
  */
@RunWith(classOf[JUnitRunner])
class AutoruUsersDaoTest extends AnyFunSuite with InitTestDbs with BeforeAndAfterAll {

  initOldSalesDbs()

  val autoruUsersDao = components.autoruUsersDao
  val autoruSalesDao = components.autoruSalesDao
  val saleId = 1043045004L
  val userId = 10591660L

  test("recallWithReason") {
    implicit val t = Traced.empty
    val updated = new DateTime(2016, 11, 16, 17, 45, 1, 0)
    val rr = RecallReason(
      saleId,
      userId,
      RecallUtils.getIdByReason(CommonModel.RecallReason.SOLD_ON_AUTORU),
      manyCalls = true,
      updated,
      Some(1000000L)
    )

    testRecallReason(rr)
    testRecallReason(rr.copy(price = None))
  }

  def testRecallReason(rr: ru.yandex.vos2.autoru.model.RecallReason)(implicit trace: Traced): Unit = {
    autoruUsersDao.upsertRecallReason(rr)
    val updatedOffer = autoruSalesDao.getOffer(rr.saleId).value
    assert(updatedOffer.recallReason.value == rr)
  }
}
