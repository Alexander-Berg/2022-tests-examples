package ru.yandex.vos2.autoru.dao.old

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class AutoruSalesDaoComtransTest extends AnyFunSuite with InitTestDbs with OptionValues with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initOldSalesDbs()
  }

  test("removePhones") {
    val saleId = 6229746
    val phone = 79299621531L
    val sale1 = components.autoruTrucksDao.getOfferForMigration(saleId).value
    assert(sale1.phones.length == 1)
    assert(sale1.phones.head.phone == phone)
    components.autoruTrucksDao.removePhones(saleId, Seq(phone.toString))(Traced.empty)

    val sale2 = components.autoruTrucksDao.getOfferForMigration(saleId).value
    assert(sale2.phones.isEmpty)
  }
}
