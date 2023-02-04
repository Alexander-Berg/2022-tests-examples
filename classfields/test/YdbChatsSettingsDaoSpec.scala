package ru.yandex.vertis.general.wisp.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.wisp.model.ChatsSettings
import ru.yandex.vertis.general.wisp.storage.ChatsSettingsDao
import ru.yandex.vertis.general.wisp.storage.ydb.YdbChatsSettingsDao
import zio.UIO
import zio.clock.Clock
import zio.test.{assert, assertCompletes, checkNM, suite, testM, DefaultRunnableSpec, Gen, ZSpec}
import zio.test.Assertion.{equalTo, isSome}

object YdbChatsSettingsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YdbChatsSettingsDao")(
      testM("Update and get chats settings") {
        checkNM(1)(Gen.anyLong) { userId =>
          for {
            settings <- UIO(ChatsSettings(false, ChatsSettings.Enabled))
            _ <- runTx(ChatsSettingsDao.updateSettings(userId, settings))
            got <- runTx(ChatsSettingsDao.getSettings(userId))
          } yield assert(got)(isSome(equalTo(settings)))
        }
      }
    )
      .provideCustomLayerShared {
        TestYdb.ydb >>> (YdbChatsSettingsDao.live ++ Ydb.txRunner) ++ Clock.live
      }
}
