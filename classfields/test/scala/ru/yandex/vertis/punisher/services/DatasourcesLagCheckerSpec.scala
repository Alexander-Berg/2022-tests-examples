package ru.yandex.vertis.punisher.services
import java.time.ZonedDateTime

import cats.Applicative
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.dao.LagDao
import ru.yandex.vertis.punisher.model.Datasources._
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model.{TaskContext, TaskDomainImpl}
import ru.yandex.vertis.punisher.services.DatasourcesLagChecker.MemoryDatasourceLagRegistry
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval

import scala.concurrent.duration._

/**
  * @author kusaeva
  */
@RunWith(classOf[JUnitRunner])
class DatasourcesLagCheckerSpec extends BaseSpec {
  case class MockLagDao(lag: F[Duration]) extends LagDao[F]

  val lagChecker: DatasourcesLagChecker[F] = {
    val autoruLag = 50
    val vertisRealtyLag = 2
    val holocronRealtyLag = 70
    val ytAutoruDao = MockLagDao(Applicative[F].pure(autoruLag.minutes))
    val ytVertisRealtyDao = MockLagDao(Applicative[F].pure(vertisRealtyLag.minutes))
    val ytHolocronRealtyDao = MockLagDao(Applicative[F].pure(holocronRealtyLag.minutes))

    val now = ZonedDateTime.now

    val lagRegistry = MemoryDatasourceLagRegistry
    lagRegistry.put(YtCarsHolocronAutoru, now.minusMinutes(autoruLag))
    lagRegistry.put(YtFactsRealty, now.minusMinutes(vertisRealtyLag))
    lagRegistry.put(YtHolocronRealty, now.minusMinutes(holocronRealtyLag))

    val daosMap: Map[Datasource, LagDao[F]] =
      Map(
        YtCarsHolocronAutoru -> ytAutoruDao,
        YtFactsRealty -> ytVertisRealtyDao,
        YtHolocronRealty -> ytHolocronRealtyDao
      )

    new DatasourcesLagCheckerImpl(lagRegistry, daosMap)
  }

  val context: TaskContext.Batch =
    TaskContext.Batch(
      taskDomain = TaskDomainImpl(Domain.DOMAIN_UNKNOWN, Labels.Lags),
      timeInterval = TimeInterval(from = ZonedDateTime.now.minusHours(2), to = ZonedDateTime.now.minusHours(1))
    )

  "DatasourcesLagChecker" should {
    "consider datasource as actual when lag is acceptable" in {

      val datasources: Seq[Datasource] = Seq(YtCarsHolocronAutoru)
      lagChecker.isRelevant(datasources)(context) shouldBe true
    }

    "consider datasource as not actual when lag is too high" in {

      val datasources: Seq[Datasource] = Seq(YtFactsRealty, YtHolocronRealty)
      lagChecker.isRelevant(datasources)(context) shouldBe false
    }

    "consider datasource as not actual when lag was not registered" in {

      val datasources: Seq[Datasource] = Seq(ModerationLog)
      lagChecker.isRelevant(datasources)(context) shouldBe false
    }
  }
}
