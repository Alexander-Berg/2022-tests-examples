package ru.yandex.realty.cashbox.dao

import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.realty.cashbox.gen.CashboxModelsGen
import ru.yandex.realty.cashbox.model.Spirit
import ru.yandex.realty.cashbox.model.enums.ReceiptStatus
import ru.yandex.vertis.util.time.DateTimeUtil

// Tests to run manually. To run it in Teamcity uncomment following line
// @RunWith(classOf[JUnitRunner])
class CashboxDaoSpec extends CashboxDaoBase with CashboxModelsGen {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(20, Millis))

  before {
    createTables()
  }

  after {
    dropTables()
  }

  "Cashbox DAOs" should {
    "insert, select, update receipts" in {

      forAll(list(5, 10, receiptGen)) { receipts =>
        receiptDao.create(receipts).futureValue

        receipts.foreach { receipt =>
          val r = receiptDao.get(receipt.receiptId).futureValue
          assert(r.receiptId == receipt.receiptId)
        }

        val now = DateTimeUtil.now()

        val testSpirit = Spirit(Some(1), Some("some-url"), Some("data"), Some("error"))

        receipts
          .map(_.copy(status = ReceiptStatus.Approved, visitTime = Some(now), spiritData = testSpirit))
          .foreach(updatedReceipt => receiptDao.update(updatedReceipt.receiptId)(_ => updatedReceipt))

        receipts.foreach { receipt =>
          val r = receiptDao.get(receipt.receiptId).futureValue
          assert(r.status == ReceiptStatus.Approved)
          assert(r.visitTime.contains(now))
          assert(r.spiritData == testSpirit)
        }
      }
    }
  }
}
