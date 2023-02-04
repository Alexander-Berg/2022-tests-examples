package ru.yandex.vertis.billing.banker.dao

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDao.MethodRecord
import ru.yandex.vertis.billing.banker.dao.util.CleanableDao
import ru.yandex.vertis.billing.banker.util.DateTimeInterval

trait DowntimeMethodDaoSpec extends AnyWordSpec with Matchers with AsyncSpecBase with BeforeAndAfterEach {

  def payments: PaymentSystemDao with CleanableDao

  def downtimeMethods: DowntimeMethodDao

  "should update downtime_until" in {
    val methodId = "bank_card"
    val downtimeUntil = DateTimeInterval.currentDay.from

    payments
      .upsertMethod(
        MethodRecord(
          methodId,
          DateTimeInterval.currentDay.from,
          rank = 100,
          epoch = None,
          downtimeUntil = None
        )
      )
      .futureValue

    downtimeMethods.downtime(methodId, downtimeUntil).futureValue shouldBe true

    val afterDisable = payments.getMethods.futureValue
      .find(m => m.id == methodId)
      .get

    afterDisable.downtimeUntil.get shouldBe downtimeUntil

    downtimeMethods.enable(methodId).futureValue shouldBe true

    val afterEnable = payments.getMethods.futureValue
      .find(m => m.id == methodId)
      .get

    afterEnable.downtimeUntil shouldBe None
  }

  override protected def beforeEach(): Unit = {
    payments.clean().futureValue
    super.beforeEach()
  }
}
