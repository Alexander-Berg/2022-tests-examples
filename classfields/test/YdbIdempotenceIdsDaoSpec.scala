package ru.yandex.vertis.general.vasabi.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.gost.model.testkit.OfferGen
import ru.yandex.vertis.general.vasabi.public.VasIds
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.vasabi.storage.IdempotenceIdsDao
import ru.yandex.vertis.general.vasabi.storage.ydb.YdbIdempotenceIdsDao
import zio.{ZIO, ZLayer}
import zio.test.{checkNM, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test._

import java.time.Instant

import scala.concurrent.duration._

object YdbIdempotenceIdsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbIdempotenceIdsDao")(
      testM("Вставка, извлечение, удаление") {
        checkNM(1)(OfferGen.anyOfferId.noShrink.map(_.id), SellerGen.anyUserId.noShrink) { (offerId, seller) =>
          for {
            dao <- ZIO.service[IdempotenceIdsDao.Service]
            _ <- runTx(dao.insertId(seller.id, offerId, VasIds.RaiseVas, "1234", Instant.ofEpochSecond(100)))
            found <- runTx(dao.getId(seller.id, offerId, VasIds.RaiseVas))
            _ <- found.map(id => runTx(dao.invalidate(id))).getOrElse(ZIO.unit)
            notFound <- runTx(dao.getId(seller.id, offerId, VasIds.RaiseVas))
          } yield assert(found)(isSome) &&
            assert(notFound)(isNone)
        }
      }
    )
  }.provideCustomLayerShared {
    val config = ZLayer.succeed(YdbIdempotenceIdsDao.Config(10.seconds))
    val ydb = TestYdb.ydb
    val dao = ydb ++ config >>> YdbIdempotenceIdsDao.live
    dao ++ (ydb >>> Ydb.txRunner)
  }
}
