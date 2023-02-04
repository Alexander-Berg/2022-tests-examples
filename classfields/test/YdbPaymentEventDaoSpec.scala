package ru.yandex.vertis.billing.cashback.storage.ydb

import ru.yandex.vertis.billing.cashback.storage.PaymentEventDao
import common.zio.ydb.testkit.TestYdb
import zio.{ZIO, ZLayer}
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test._
import zio.test.TestAspect.{before, sequential}
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb.{runTx, ydb}
import zio.clock.Clock
import zio.test.TestAspect._
import zio.test._

object YdbPaymentEventDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = (
    suite("YdbPaymentDaoSpec")(
      testM("any") {
        for {
          ydbPaymentDao <- ZIO.service[PaymentEventDao]
          // TODO: нужно бы сделать так, чтобы таблица 1 раз создавалась. Но при этом из ydbPaymentDao не хочется делать ZLayer
          now <- zio.clock.instant
          _ <- runTx(
            ydbPaymentDao.writePaymentEvent(
              "autoru",
              "user:123",
              "WITHDRAW",
              "payment:1234",
              1,
              "payment:1234#1",
              200,
              now
            )
          )
          result <- runTx(ydbPaymentDao.getPaymentEvents("autoru", "user:123", "payment:1234"))
        } yield assertTrue(result.head.clientId == "user:123")
      }
    ) @@ sequential
  ).provideCustomLayerShared(TestYdb.ydb >+> Ydb.txRunner >+> YdbPaymentEventDao.live)
  // TODO: C zio-magic почему-то фейлится .injectCustomShared(TestYdb.ydb, YdbPaymentDao.live, Ydb.txRunner)

}
