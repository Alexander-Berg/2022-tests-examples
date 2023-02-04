package ru.yandex.vertis.parsing.auto.dao.resellers

import org.joda.time.{DateTime, DateTimeUtils}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers, OptionValues}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.common.ResellerRecordWithOffset
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.dao.DbResult
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport

@RunWith(classOf[JUnitRunner])
class ResellersDaoImplTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with EmptyTraceSupport
  with Matchers
  with BeforeAndAfter {
  initDb()

  private val resellersDao: ResellersDao = components.resellersDao

  private val now: DateTime = DateTime.now().withMillisOfDay(0)

  before {
    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

  test("skip reseller creation: not enough offers") {
    val sellerUrl = "seller1"
    val result = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 1, 500)), "test")
    result(sellerUrl) shouldBe DbResult.DidNothing

    val result2 = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 2, 501)), "test")
    result2(sellerUrl) shouldBe DbResult.DidNothing

    resellersDao.getResellers(Seq(sellerUrl)) shouldBe empty
  }

  test("should create") {
    val sellerUrl = "seller2"
    val result = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 3, 500)), "test")
    result(sellerUrl) shouldBe DbResult.Inserted

    resellersDao.getResellers(Seq(sellerUrl)) should have size 1
  }

  test("should not update: same offers") {
    val sellerUrl = "seller3"
    val result = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 3, 500)), "test")
    result(sellerUrl) shouldBe DbResult.Inserted

    resellersDao.getResellers(Seq(sellerUrl)) should have size 1

    DateTimeUtils.setCurrentMillisFixed(now.getMillis + 1000)

    val result2 = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 3, 501)), "test")
    result2(sellerUrl) shouldBe DbResult.DidNothing

    resellersDao.getResellers(Seq(sellerUrl)).head.history.history should have size 1
  }

  test("should not update: wrong offset") {
    val sellerUrl = "seller4"
    val result = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 3, 500)), "test")
    result(sellerUrl) shouldBe DbResult.Inserted

    resellersDao.getResellers(Seq(sellerUrl)) should have size 1

    DateTimeUtils.setCurrentMillisFixed(now.getMillis + 1000)

    val result2 = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 4, 499)), "test")
    result2(sellerUrl) shouldBe DbResult.DidNothing

    resellersDao.getResellers(Seq(sellerUrl)).head.history.history should have size 1
  }

  test("should update") {
    val sellerUrl = "seller5"
    val result = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 3, 500)), "test")
    result(sellerUrl) shouldBe DbResult.Inserted

    resellersDao.getResellers(Seq(sellerUrl)) should have size 1

    DateTimeUtils.setCurrentMillisFixed(now.getMillis + 1000)

    val result2 = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 2, 501)), "test")
    result2(sellerUrl) shouldBe DbResult.Updated(Set("OffersCount"))

    resellersDao.getResellers(Seq(sellerUrl)).head.history.history should have size 2
  }

  test("should update: wrong offset but different comment") {
    val sellerUrl = "seller6"
    val result = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 3, 500)), "test")
    result(sellerUrl) shouldBe DbResult.Inserted

    resellersDao.getResellers(Seq(sellerUrl)) should have size 1

    DateTimeUtils.setCurrentMillisFixed(now.getMillis + 1000)

    val result2 = resellersDao.save(Seq(ResellerRecordWithOffset(sellerUrl, 4, 499)), "test2")
    result2(sellerUrl) shouldBe DbResult.Updated(Set("OffersCount"))

    resellersDao.getResellers(Seq(sellerUrl)).head.history.history should have size 2
  }
}
