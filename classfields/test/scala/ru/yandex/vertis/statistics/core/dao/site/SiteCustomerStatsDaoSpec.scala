package ru.yandex.vertis.statistics.core.dao.site

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase2
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase2.DbWithProperties
import ru.yandex.realty.ops.DaoOperationalComponents
import ru.yandex.realty.pos.TestOperationalComponents
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.statistics.core.db.RawStatisticsJdbcSpecBase

import java.time.LocalDate
import java.time.temporal.ChronoUnit

@RunWith(classOf[JUnitRunner])
class SiteCustomerStatsDaoSpec
  extends WordSpec
  with Matchers
  with RawStatisticsJdbcSpecBase
  with AsyncSpecBase
  with TestOperationalComponents
  with DaoOperationalComponents
  with ProducerProvider {

  private lazy val dbAndThings = DbWithProperties(database, "test")
  private lazy val testDatabase = MasterSlaveJdbcDatabase2(dbAndThings, dbAndThings)
  private lazy val dao: SiteCustomerStatsDao =
    new MysqlSiteCustomerStatsDao(testDatabase, daoMetrics)

  implicit private val traced: Traced = Traced.empty

  "SiteCustomerStatsDao" should {
    "write and read statistics" in {
      val stats = statsGen.next(10).toSeq
      dao.upsert(stats).futureValue
      val fetched = dao.get(stats.map(SiteCustomerStatsKey(_))).futureValue
      fetched.map(_.copy(id = None)).toSet shouldEqual stats.toSet
    }
  }

  private lazy val statsGen: Gen[SiteCustomerStats] = for {
    siteId <- Gen.posNum[Long]
    date <- Gen.choose(1, 1000).map(d => LocalDate.now().minus(d, ChronoUnit.DAYS))
    clientId <- Gen.posNum[Long]
    agencyId <- Gen.option(Gen.posNum[Long])
    offersNumber <- Gen.option(Gen.posNum[Int])
    pricePerMeter <- Gen.option(Gen.posNum[Long])
  } yield SiteCustomerStats(None, date, siteId, clientId, agencyId, offersNumber, pricePerMeter)
}
