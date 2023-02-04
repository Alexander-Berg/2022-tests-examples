package ru.yandex.vertis.statistics.core.dao.transactions

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase
import ru.yandex.realty.proto.offer.{OfferCategory, OfferType}
import ru.yandex.realty.statistics.model.DruidRealtyTransactionRow.Product
import ru.yandex.vertis.statistics.core.db.RawStatisticsJdbcSpecBase
import ru.yandex.vertis.statistics.core.model.Transaction

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class TransactionsDaoSpec extends WordSpec with Matchers with RawStatisticsJdbcSpecBase with AsyncSpecBase {

  private lazy val testDatabase = MasterSlaveJdbcDatabase(database, database)
  private lazy val transactionsDao: TransactionsDao =
    new TransactionsDaoImpl(new TransactionsActionsImpl(), testDatabase)

  "TransactionsDao" should {
    "read transaction correctly" in {
      val trId = "826a033ce60d53f058136ac4c5da7318"
      val t = Transaction(
        id = trId,
        timestamp = new DateTime("2020-08-16T15:00:00.000+03:00"),
        product = Product.PLACEMENT,
        price = 100L,
        discountedPrice = 100L,
        ownerUid = Some("361344904"),
        offerPartnerId = Some("1069188070"),
        offerType = Some(OfferType.SELL),
        offerCategory = Some(OfferCategory.GARAGE),
        subjectFederationId = Some(977),
        offerId = Some("5434297821686427500"),
        callId = None,
        paidReportId = None,
        address = None
      )
      transactionsDao.upsert(t).futureValue
      transactionsDao.upsert(t).futureValue
      val res = transactionsDao.get(trId).futureValue
      res.get shouldEqual (t)
    }
  }
}
