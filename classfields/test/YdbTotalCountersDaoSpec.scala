package ru.yandex.vertis.general.gost.storage.test

import zio.test._
import zio.test.Assertion._
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.gost.model.counters.{Counter, CountersAggregate}
import ru.yandex.vertis.general.gost.storage.TotalCountersDao
import ru.yandex.vertis.general.gost.storage.ydb.counters.YdbTotalCountersDao
import zio.clock.Clock

object YdbTotalCountersDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YdbTotalCountersDao")(
      testM("Save counter in base and get it") {
        val sellerId: SellerId = SellerId.UserId(17)
        val aggregate = CountersAggregate(Map(Counter.Active -> 10))
        val counters = Map(sellerId -> aggregate)
        for {
          _ <- runTx(TotalCountersDao.updateTotalCounters(counters))
          result <- runTx(TotalCountersDao.getSellerTotalCounters(sellerId))
        } yield assert(result)(equalTo(aggregate))
      },
      testM("Save counter multiple times for one seller") {
        val sellerId: SellerId = SellerId.UserId(27)
        val aggregate = CountersAggregate(Map(Counter.Active -> 10))
        val aggregate2 = CountersAggregate(Map(Counter.Active -> 5))
        val counters = Map(sellerId -> aggregate)
        val counters2 = Map(sellerId -> aggregate2)
        val expected = CountersAggregate(Map(Counter.Active -> 15))
        for {
          _ <- runTx(TotalCountersDao.updateTotalCounters(counters))
          _ <- runTx(TotalCountersDao.updateTotalCounters(counters2))
          result <- runTx(TotalCountersDao.getSellerTotalCounters(sellerId))
        } yield assert(result)(equalTo(expected))
      },
      testM("Save multiple counters and get them") {
        val sellerId: SellerId = SellerId.UserId(177)
        val aggregate = CountersAggregate(Map(Counter.Active -> 10))
        val aggregate2 = CountersAggregate(Map(Counter.Expired -> 5))
        val counters = Map(sellerId -> aggregate)
        val counters2 = Map(sellerId -> aggregate2)
        val expected = aggregate + aggregate2
        for {
          _ <- runTx(TotalCountersDao.updateTotalCounters(counters))
          _ <- runTx(TotalCountersDao.updateTotalCounters(counters2))
          result <- runTx(TotalCountersDao.getSellerTotalCounters(sellerId))
        } yield assert(result)(equalTo(expected))
      },
      testM("Save multiple counters for multiple sellers and get them") {
        val sellerId1: SellerId = SellerId.UserId(1777)
        val sellerId2: SellerId = SellerId.UserId(17777)
        val aggregate1 = CountersAggregate(Map(Counter.Active -> 10))
        val aggregate2 = CountersAggregate(Map(Counter.Expired -> 5))
        val counters = Map(sellerId1 -> aggregate1, sellerId2 -> aggregate2)
        for {
          _ <- runTx(TotalCountersDao.updateTotalCounters(counters))
          result1 <- runTx(TotalCountersDao.getSellerTotalCounters(sellerId1))
          result2 <- runTx(TotalCountersDao.getSellerTotalCounters(sellerId2))
        } yield assert(result1)(equalTo(aggregate1)) && assert(result2)(equalTo(aggregate2))
      }
    )
      .provideCustomLayerShared(
        TestYdb.ydb >>> (YdbTotalCountersDao.live ++ Ydb.txRunner) ++ Clock.live
      )
}
