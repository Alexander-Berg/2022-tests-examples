package ru.yandex.vertis.general.favorites.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.favorites.notification_model.NotificationChannel.{EMAIL, PUSH}
import general.favorites.notification_model.NotificationSettings
import ru.yandex.vertis.general.favorites.model.UserNotificationSettings
import ru.yandex.vertis.general.favorites.storage.NotificationSettingsDao
import ru.yandex.vertis.general.favorites.storage.ydb.settings.YdbNotificationSettingsDao
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object YdbNotificationSettingsDaoTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbNotificationSettingsTest")(
      testM("Получение и сохранение настроек") {
        val userId = 123L
        for {
          initialSettings <- runTx(NotificationSettingsDao.getUserNotificationSettings(userId))
          _ <- runTx(
            NotificationSettingsDao.upsertUserNotificationSettings(
              userId,
              UserNotificationSettings(
                NotificationSettings(EMAIL :: Nil),
                savedSearchNotificationsEnabledByDefault = false,
                savedSellerNotificationsEnabledByDefault = false,
                NotificationSettings(EMAIL :: Nil)
              )
            )
          )
          _ <- runTx(
            NotificationSettingsDao.upsertUserNotificationSettings(
              userId,
              UserNotificationSettings(
                NotificationSettings(PUSH :: Nil),
                savedSearchNotificationsEnabledByDefault = true,
                savedSellerNotificationsEnabledByDefault = true,
                NotificationSettings(Nil)
              )
            )
          )
          updatedSettings <- runTx(NotificationSettingsDao.getUserNotificationSettings(userId))
        } yield assert(initialSettings)(isNone) && assert(updatedSettings)(
          isSome(
            equalTo(
              UserNotificationSettings(
                NotificationSettings(PUSH :: Nil),
                savedSearchNotificationsEnabledByDefault = true,
                savedSellerNotificationsEnabledByDefault = true,
                NotificationSettings(Nil)
              )
            )
          )
        )

      }
    ).provideCustomLayer {
      TestYdb.ydb >>> (YdbNotificationSettingsDao.live ++ Ydb.txRunner) ++ Clock.live
    } @@ sequential @@ shrinks(0)
  }
}
