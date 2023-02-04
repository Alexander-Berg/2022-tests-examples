package ru.yandex.vertis.statistics.core.dao.archive

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase
import ru.yandex.realty.proto.offer.{OfferCategory, OfferType}
import ru.yandex.vertis.statistics.core.db.ArchiveJdbcSpecBase
import slick.jdbc.MySQLProfile.api._

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class OfferDaoSpec extends WordSpec with Matchers with ArchiveJdbcSpecBase with AsyncSpecBase {
  private lazy val testDatabase = MasterSlaveJdbcDatabase(database, database)
  private lazy val offerDao: OfferDao = new OfferDaoImpl(testDatabase)

  "OfferDao" should {
    "read offer type, offer category, subject federation" in {
      val offerId = "5434297821686427500"
      val o = Offer(
        id = 0,
        offerId = offerId,
        offerType = OfferType.SELL,
        offerCategory = OfferCategory.GARAGE,
        subjectFederationId = Some(977),
        regionGraphId = Some(682310L)
      )
      val insertAction = OfferTable.offerTable.insertOrUpdate(o)
      testDatabase.master.run(insertAction).futureValue

      val res = offerDao.get(offerId).futureValue
      res.nonEmpty shouldBe (true)
      res.get.offerType shouldBe (OfferType.SELL)
      res.get.offerCategory shouldBe (OfferCategory.GARAGE)
      res.get.subjectFederationId shouldBe Some(977)
      res.get.regionGraphId shouldBe Some(682310L)
    }
  }
}
