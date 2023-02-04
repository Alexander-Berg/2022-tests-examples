package vertis.pica

import vertis.pica.model.Host
import zio.test._
import zio.test.Assertion._
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import zio.clock.Clock

class YdbHostQueueCounterDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbHostQueueCounterDao")(
      testM("get nothing for a non-existent host") {
        val host: Host = "auto.ru"
        for {
          result <- runTx(HostQueueCounterDao.get(host))
        } yield assert(result)(equalTo(None))
      },
      testM("save counter in db and get it") {
        val host: Host = "auto.ru"
        val value = 10L
        for {
          _ <- runTx(HostQueueCounterDao.update(host, value))
          result <- runTx(HostQueueCounterDao.get(host))
        } yield assert(result)(equalTo(Some(value)))
      },
      testM("save counter multiple times for one host") {
        val host: Host = "yandex.realty.ru"
        val value1 = 10L
        val value2 = 25L
        val expected = value1 + value2
        for {
          _ <- runTx(HostQueueCounterDao.update(host, value1))
          _ <- runTx(HostQueueCounterDao.update(host, value2))
          result <- runTx(HostQueueCounterDao.get(host))
        } yield assert(result)(equalTo(Some(expected)))
      },
      testM("save multiple counters for multiple hots and get them") {
        val host1: Host = "auto.ru"
        val value11 = 1L
        val value12 = 25L
        val expected1 = value11 + value12
        val host2: Host = "yandex.realty.ru"
        val value21 = 30L
        val value22 = 15L
        val expected2 = value21 + value22
        for {
          _ <- runTx(HostQueueCounterDao.update(host1, value11))
          _ <- runTx(HostQueueCounterDao.update(host1, value12))
          _ <- runTx(HostQueueCounterDao.update(host2, value21))
          _ <- runTx(HostQueueCounterDao.update(host2, value22))
          result1 <- runTx(HostQueueCounterDao.get(host1))
          result2 <- runTx(HostQueueCounterDao.get(host2))
        } yield assert(result1)(equalTo(Some(expected1))) && assert(result2)(equalTo(Some(expected2)))
      }
    )
      .provideCustomLayerShared(
        TestYdb.ydb >>> (YdbHostQueueCounterDao.live ++ Ydb.txRunner) ++ Clock.live
      )
  }

}
