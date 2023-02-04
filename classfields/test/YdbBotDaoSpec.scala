package ru.yandex.vertis.general.wisp.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.wisp.model.BotData
import ru.yandex.vertis.general.wisp.storage.BotDao
import ru.yandex.vertis.general.wisp.storage.ydb.YdbBotDao
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isSome}
import zio.test.{assert, suite, testM, DefaultRunnableSpec, ZSpec}

object YdbBotDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YdbBotDao")(
      testM("Create bot and get it by userId") {
        for {
          botData <- ZIO.succeed(BotData("uid", "bid", "guid"))
          _ <- runTx(BotDao.createBot(botData))
          created <- runTx(BotDao.getBot("uid"))
        } yield assert(created)(isSome(equalTo(botData)))
      }
    )
      .provideCustomLayerShared(
        TestYdb.ydb >>> (YdbBotDao.live ++ Ydb.txRunner) ++ Clock.live
      )
}
