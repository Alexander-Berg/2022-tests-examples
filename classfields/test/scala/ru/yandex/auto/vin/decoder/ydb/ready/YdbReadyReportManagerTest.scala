package ru.yandex.auto.vin.decoder.ydb.ready

import auto.carfax.common.utils.tracing.Traced
import auto.carfax.pro_auto.core.src.testkit.YdbContainerKit
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.VinReportModel.{LegalBlock, PtsBlock, RawVinEssentialsReport}
import ru.auto.api.vin.VinResolutionEnums
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.ydb.ready.model.YdbReadyReportRow
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import ru.yandex.vertis.ydb.{QueryOptions, YdbContainer}
import zio.Runtime
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class YdbReadyReportManagerTest
  extends AnyWordSpecLike
  with YdbContainerKit
  with MockitoSupport
  with ForAllTestContainer
  with Matchers {

  implicit val t: Traced = Traced.empty

  private val zioRuntime: zio.Runtime[Blocking with Clock with Random] = Runtime.default

  private val prefix = "/local"

  private lazy val ydb = YdbZioWrapper.make(container.tableClient, prefix, 3.seconds, QueryOptions.Default.withV1Syntax)

  private lazy val reportDao = new YdbReadyReportDao(ydb, zioRuntime, None)
  private lazy val reportManager = new YdbReadyReportManager(ydb, zioRuntime, reportDao, TestOperationalSupport)

  private def daoTest(action: YdbReadyReportDao => Any): Unit = {
    action(reportDao): Unit
  }

  private def managerTest(action: YdbReadyReportManager => Any): Unit = {
    action(reportManager): Unit
  }

  "YdbReadyReportManager" should {
    "init" in daoTest { dao =>
      dao.init()
    }

    "insert report" in managerTest { manager =>
      val vin = VinCode("XTA217030B0276237")
      val report = RawVinEssentialsReport
        .newBuilder()
        .setPtsInfo(PtsBlock.newBuilder().setVin(vin.toString))
        .setLegal(LegalBlock.newBuilder().setConstraintsStatus(VinResolutionEnums.Status.ERROR))
        .build()
      val reportFromDbF = for {
        _ <- manager.upsertReport(vin, report)
        storedReport <- manager.getReport(vin)
      } yield storedReport

      val reportFromDb = Await.result(reportFromDbF, 10.seconds)

      report shouldBe reportFromDb.get
    }

    "get unexisting report" in managerTest { manager =>
      val vin = VinCode("XTA217030B0276238")
      val res = Await.result(manager.getReport(vin), 10.second)
      res shouldBe None
    }

    "insert batch" in managerTest { manager =>
      val vin1 = VinCode("XTA217030B0276237")
      val report1 = RawVinEssentialsReport
        .newBuilder()
        .setPtsInfo(PtsBlock.newBuilder().setVin(vin1.toString))
        .setLegal(LegalBlock.newBuilder().setConstraintsStatus(VinResolutionEnums.Status.ERROR))
        .build()

      val vin2 = VinCode("XTA217030B0276238")
      val report2 = RawVinEssentialsReport
        .newBuilder()
        .setPtsInfo(PtsBlock.newBuilder().setVin(vin2.toString))
        .setLegal(LegalBlock.newBuilder().setConstraintsStatus(VinResolutionEnums.Status.ERROR))
        .build()

      val reportRows = Seq(
        YdbReadyReportRow(vin1, report1),
        YdbReadyReportRow(vin2, report2)
      )

      val reportFromDbF = for {
        _ <- manager.upsertBatch(reportRows)
        storedReport1 <- manager.getReport(vin1)
        storedReport2 <- manager.getReport(vin2)
      } yield (storedReport1, storedReport2)

      val reportFromDb = Await.result(reportFromDbF, 10.seconds)

      reportFromDb._1.get shouldBe report1
      reportFromDb._2.get shouldBe report2
    }

    "drop" in daoTest { dao =>
      dao.drop()
    }
  }

}
