package ru.yandex.realty.notifier.manager

import java.time.Instant

import org.junit.runner.RunWith
import org.scalatest.Assertion
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.notifier.application.DefaultNotificationSettingsManagerProvider
import ru.yandex.realty.notifier.db.{BaseDbSpec, CleanSchemaBeforeAll}
import ru.yandex.realty.notifier.manager.NotificationSettingsManager.{
  RentMarketingCampaignsTopicId,
  RentServiceNotificationsTopicId
}
import ru.yandex.realty.notifier.model.RentUserTarget
import ru.yandex.realty.notifier.proto.model.settings.NotificationChannelNamespace.{
  NotificationChannel => ModelNotificationChannel
}
import ru.yandex.realty.proto.notifier.api.NotificationChannelNamespace.NotificationChannel
import ru.yandex.realty.proto.notifier.api.{
  ChannelSettings,
  InternalPatchNotificationSettingsRequest,
  NotificationSettings,
  NotificationSettingsPatch
}
import ru.yandex.vertis.generators.BasicGenerators

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class NotificationSettingsManagerSpec
  extends SpecBase
  with BaseDbSpec
  with CleanSchemaBeforeAll
  with DefaultNotificationSettingsManagerProvider
  with BasicGenerators {

  "NotificationSettingsManager" should {
    "throw error if notification topic does not exist" in {
      notificationSettingsManager
        .canSendNotification(RentUserTarget(1), "this_topic_does_not_exist", ModelNotificationChannel.SMS)
        .failed
        .futureValue shouldBe a[IllegalArgumentException]
    }

    "get default settings for new user" in {
      val uid = posNum[Long].next

      val settings = notificationSettingsManager
        .getNotificationSettings(userUid = uid)
        .futureValue

      val rentTopic = settings.getSuccess.getTopicsList.asScala
        .find(_.getTopicId == RentServiceNotificationsTopicId)

      rentTopic shouldBe defined
      rentTopic.get.getTitle shouldBe "Яндекс.Аренда"
      rentTopic.get.getChannelsList.asScala should contain(
        ChannelSettings.newBuilder().setEnabled(true).setChannel(NotificationChannel.SMS).build()
      )
      rentTopic.get.getChannelsList.asScala should contain(
        ChannelSettings.newBuilder().setEnabled(true).setChannel(NotificationChannel.EMAIL).build()
      )
    }

    "patch settings" in {
      val uid = posNum[Long].next

      val request1 = createPatchRequest(
        List(
          createSettingsPatch(NotificationChannel.EMAIL, enabled = false),
          createSettingsPatch(NotificationChannel.SMS, enabled = false)
        )
      )
      val request2 = createPatchRequest(List(createSettingsPatch(NotificationChannel.SMS, enabled = true)))

      def checkSettings(settings: NotificationSettings): Assertion = {
        val rentTopic = settings.getTopicsList.asScala
          .find(_.getTopicId == RentServiceNotificationsTopicId)

        rentTopic shouldBe defined
        rentTopic.get.getTitle shouldBe "Яндекс.Аренда"
        rentTopic.get.getChannelsList.asScala should contain(
          ChannelSettings.newBuilder().setEnabled(true).setChannel(NotificationChannel.SMS).build()
        )
        rentTopic.get.getChannelsList.asScala should contain(
          ChannelSettings.newBuilder().setEnabled(false).setChannel(NotificationChannel.EMAIL).build()
        )
      }

      notificationSettingsManager
        .patchNotificationSettings(userUid = uid, request = request1)
        .futureValue
      val settingsAfterPatch = notificationSettingsManager
        .patchNotificationSettings(userUid = uid, request = request2)
        .futureValue

      checkSettings(settingsAfterPatch.getSuccess)

      val settingsAfterGet = notificationSettingsManager
        .getNotificationSettings(userUid = uid)
        .futureValue
        .getSuccess

      checkSettings(settingsAfterGet)
    }

    "set update time on patch" in {
      val uid = posNum[Long].next

      val defaultSettings = notificationSettingsManager
        .internalGetNotificationSettings(userUid = uid)
        .futureValue
      val defaultMarketing = defaultSettings.getTopicsList.asScala.find(_.getTopicId == RentMarketingCampaignsTopicId)
      val defaultService = defaultSettings.getTopicsList.asScala.find(_.getTopicId == RentServiceNotificationsTopicId)

      defaultMarketing.get.hasUpdateTime shouldBe false
      defaultService.get.hasUpdateTime shouldBe false

      //--------------
      // Update marketing topic

      val firstRequest = createPatchRequest(
        List(createSettingsPatch(NotificationChannel.SMS, enabled = true, topicId = RentMarketingCampaignsTopicId))
      )

      notificationSettingsManager.patchNotificationSettings(userUid = uid, request = firstRequest).futureValue
      val settingsAfterUpdate1 = notificationSettingsManager
        .internalGetNotificationSettings(userUid = uid)
        .futureValue
      val marketingAfterUpdate1 =
        settingsAfterUpdate1.getTopicsList.asScala.find(_.getTopicId == RentMarketingCampaignsTopicId)
      val serviceAfterUpdate1 =
        settingsAfterUpdate1.getTopicsList.asScala.find(_.getTopicId == RentServiceNotificationsTopicId)

      marketingAfterUpdate1.get.hasUpdateTime shouldBe true
      val updateTimeDelta = math.abs(marketingAfterUpdate1.get.getUpdateTime.getSeconds - Instant.now().getEpochSecond)
      (updateTimeDelta < 100) shouldBe true
      serviceAfterUpdate1.get.hasUpdateTime shouldBe false

      //--------------
      // Update service topic

      val secondRequest = createPatchRequest(
        List(createSettingsPatch(NotificationChannel.SMS, enabled = true, topicId = RentServiceNotificationsTopicId))
      )

      notificationSettingsManager.patchNotificationSettings(userUid = uid, request = secondRequest).futureValue
      val settingsAfterUpdate2 = notificationSettingsManager
        .internalGetNotificationSettings(userUid = uid)
        .futureValue
      val marketingAfterUpdate2 =
        settingsAfterUpdate2.getTopicsList.asScala.find(_.getTopicId == RentMarketingCampaignsTopicId)
      val serviceAfterUpdate2 =
        settingsAfterUpdate2.getTopicsList.asScala.find(_.getTopicId == RentServiceNotificationsTopicId)

      marketingAfterUpdate2.get.hasUpdateTime shouldBe true
      marketingAfterUpdate2.get.getUpdateTime shouldBe marketingAfterUpdate1.get.getUpdateTime
      serviceAfterUpdate2.get.hasUpdateTime shouldBe true

    }

  }

  private def createPatchRequest(patches: List[NotificationSettingsPatch]): InternalPatchNotificationSettingsRequest =
    InternalPatchNotificationSettingsRequest
      .newBuilder()
      .addAllPatch(
        patches.asJava
      )
      .build()

  private def createSettingsPatch(
    channel: NotificationChannel,
    enabled: Boolean,
    topicId: String = RentServiceNotificationsTopicId
  ): NotificationSettingsPatch =
    NotificationSettingsPatch
      .newBuilder()
      .setTopicId(topicId)
      .setChannel(channel)
      .setEnabled(enabled)
      .build()
}
