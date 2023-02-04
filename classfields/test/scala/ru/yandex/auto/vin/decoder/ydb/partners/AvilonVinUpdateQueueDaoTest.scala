package ru.yandex.auto.vin.decoder.ydb.partners

import auto.carfax.pro_auto.core.src.testkit.YdbContainerKit
import com.dimafeng.testcontainers.ForAllTestContainer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.utils.concurrent.CoreFutureUtils._
import ru.yandex.vertis.ydb.{QueryOptions, YdbContainer}
import ru.yandex.vertis.ydb.zio.{TxIO, YdbZioWrapper}
import zio.{CancelableFuture, Runtime}

import scala.concurrent.duration._

class AvilonVinUpdateQueueDaoTest extends AnyWordSpecLike with YdbContainerKit with Matchers with ForAllTestContainer {

  lazy val zioRuntime: Runtime[zio.ZEnv] = Runtime.default

  lazy val ydb: YdbZioWrapper =
    YdbZioWrapper.make(container.tableClient, "/local", 3.seconds, QueryOptions.Default.withV1Syntax)

  private lazy val avilonVinsUpdateQueueDao = new AvilonVinUpdateQueueDao(ydb, zioRuntime, "partners")

  implicit class TxIOOps[T](txIO: TxIO[Throwable, T]) {

    def execute: CancelableFuture[T] = {
      val res = ydb.runTx(txIO).mapError(_.squash)
      zioRuntime.unsafeRunToFuture(res)
    }
  }

  "AvilonVinUpdateQueueDao" should {

    val vin1 = VinCode("Z8T4C5FS9BM005269")
    val vin2 = VinCode("ABCDEFGHJKLMNPRST")

    "init" in {
      avilonVinsUpdateQueueDao.init()
    }

    "batchUpsert" should {

      "insert new vins" in {
        avilonVinsUpdateQueueDao.batchUpsert(List(vin1, vin2)).execute.await
        val vins = avilonVinsUpdateQueueDao.getVins(100500).execute.await

        (vins should have).length(2)
        vins should contain(vin1)
        vins should contain(vin2)
      }

      "do not insert new vin then it is already exists" in {
        avilonVinsUpdateQueueDao.batchUpsert(List(vin1, vin2)).execute.await
        val vins = avilonVinsUpdateQueueDao.getVins(100500).execute.await

        (vins should have).length(2)
        vins should contain(vin1)
        vins should contain(vin2)
      }
    }

    "get vins" in {
      val vins = avilonVinsUpdateQueueDao.getVins(2).execute.await

      (vins should have).length(2)
      vins should contain(vin1)
      vins should contain(vin2)
    }

    "delete vin" in {
      avilonVinsUpdateQueueDao.deleteVin(vin1).execute.await
      val vins = avilonVinsUpdateQueueDao.getVins(100500).execute.await

      (vins should have).length(1)
      vins should contain(vin2)
    }

    "drop" in {
      avilonVinsUpdateQueueDao.drop()
    }
  }
}
