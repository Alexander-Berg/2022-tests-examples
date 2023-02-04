package ru.yandex.realty.managers.notification

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.notifier.NotifierClient
import ru.yandex.realty.clients.watch.{WatchClient, WatchGenerators}
import ru.yandex.realty.controllers.Watches
import ru.yandex.realty.controllers.Watches.RichDeliveries
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.managers.chat.push.message.ChatNewMessage
import ru.yandex.realty.model.user.{PassportUser, UserInfo, UserRef, UserRefGenerators}
import ru.yandex.realty.proto.notifier.api.NotificationChannelNamespace.NotificationChannel
import ru.yandex.realty.proto.notifier.api.{
  ChannelSettings,
  InternalGetNotificationSettingsResponse,
  InternalPatchNotificationSettingsRequest,
  InternalPatchNotificationSettingsResponse,
  NotificationSettings,
  NotificationSettingsPatch,
  NotificationTopic
}
import ru.yandex.realty.pushnoy.PushnoyClient
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.user.notification.NotificationConfiguration.{
  ChatNewMessageId,
  ChatNewMessageTitle,
  RentNotificationsId,
  RentNotificationsTitle
}
import ru.yandex.realty.user.notification.ProtoNotification.{DeliveryMethod, DeliveryType}
import ru.yandex.realty.user.notification.{DeliveryTypes, NotificationConfiguration, ProtoNotification}
import ru.yandex.vertis.subscriptions.api.ApiModel
import ru.yandex.realty.util.Mappings._

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * Specs on [[NotificationsManager]]
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class NotificationsManagerSpec extends AsyncSpecBase with RequestAware with UserRefGenerators with WatchGenerators {

  private val watches = mock[WatchClient]
  private val pushnoy = mock[PushnoyClient]
  private val notifier = mock[NotifierClient]
  private val manager = new NotificationsManager(watches, pushnoy, notifier)

  private val AllDeliveryTypes = Set(DeliveryTypes.Push, DeliveryTypes.Sms, DeliveryTypes.Email)

  private def getDisabledWatch(userRef: UserRef) =
    ApiModel.Watch
      .newBuilder()
      .setOwner(userRef.toPlain)
      .setDeliveries(
        Watches
          .defaultDeliveries(userRef)
          .setDeliveryEnabled(DeliveryTypes.Push, enabled = false)
          .applyTransformIf(
            userRef.isInstanceOf[PassportUser],
            _.setDeliveryEnabled(DeliveryTypes.Email, enabled = false)
          )
      )
      .build

  "NotificationManager" should {
    "create watch in case of edit non-existent watch delivery" in {
      val user = passportUserGen.next
      val userInfo = UserInfo("", 0, None, None, user)

      withRequestContext(user) { implicit r =>
        val patch = ProtoNotification.NotificationConfiguration
          .newBuilder()
          .setId(NotificationConfiguration.OfferChangesId)
          .setTitle(NotificationConfiguration.OfferChangesTitle)
          .addMethods(
            DeliveryMethod
              .newBuilder()
              .setDeliveryType(DeliveryType.DELIVERY_TYPE_EMAIL)
              .setEnabled(false)
          )
          .build()

        (watches
          .setDeliveryEnabled(_: UserRef, _: DeliveryTypes.Value, _: Boolean)(_: Traced))
          .expects(user, DeliveryTypes.Email, false, *)
          .returning(Future.failed(new NoSuchElementException))

        val watch = ApiModel.Watch
          .newBuilder()
          .setOwner(user.toPlain)
          .setDeliveries(
            Watches
              .defaultDeliveries(user)
              .setDeliveryEnabled(DeliveryTypes.Email, enabled = false)
          )
          .build

        (watches
          .patchWatch(_: UserRef, _: ApiModel.WatchPatch, _: ApiModel.Deliveries)(_: Traced))
          .expects(user, *, *, *)
          .returning(Future.successful(watch))

        val configuration: ProtoNotification.NotificationConfiguration = manager
          .update(userInfo, patch)
          .futureValue

        configuration.getMethodsList.asScala
          .filter(_.getDeliveryType == DeliveryType.DELIVERY_TYPE_EMAIL)
          .foreach(_.getEnabled should be(false))
        configuration.getMethodsList.asScala
          .filter(_.getDeliveryType == DeliveryType.DELIVERY_TYPE_PUSH)
          .foreach(_.getEnabled should be(true))
      }
    }

    "return disabled chatNewMessagePush when subscription removed in pushnoy" in {
      val appUser = appUserGen.next
      val appUserInfo = UserInfo("", 0, Some(appUser.uuid), None, appUser)

      val watch = getDisabledWatch(appUser)

      withRequestContext(appUser) { implicit r =>
        {
          (pushnoy
            .getDeviceRemovedSubscriptions(_: String)(_: Traced))
            .expects(appUser.uuid, *)
            .returning(Future.successful(Set(ChatNewMessageId)))
          (watches
            .getWatch(_: UserRef)(_: Traced))
            .expects(appUser, *)
            .returning(Future.successful(watch))

          val configurations: ProtoNotification.NotificationConfigurations =
            manager.current(appUserInfo, AllDeliveryTypes).futureValue
          val chatNewMessageConfigurationOpt =
            configurations.getValuesList.asScala.find(_.getId == ChatNewMessageId)
          chatNewMessageConfigurationOpt.isDefined shouldBe true

          chatNewMessageConfigurationOpt.get shouldBe ProtoNotification.NotificationConfiguration
            .newBuilder()
            .setId(ChatNewMessageId)
            .setTitle("Новое сообщение в чате")
            .addMethods(DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_PUSH).setEnabled(false))
            .build()
        }
      }

    }

    "return empty chat_new_message configuration for passport user without deviceUuid" in {
      val passportUser = passportUserGen.next
      val userInfo = UserInfo("", 0, None, None, passportUser)

      val watch = getDisabledWatch(passportUser)

      withRequestContext(passportUser) { implicit r =>
        {
          (watches
            .getWatch(_: UserRef)(_: Traced))
            .expects(passportUser, *)
            .returning(Future.successful(watch))
          (notifier
            .getNotificationSettings(_: PassportUser)(_: Traced))
            .expects(passportUser, *)
            .returning(
              Future.successful(
                InternalGetNotificationSettingsResponse
                  .newBuilder()
                  .setSuccess(NotificationSettings.getDefaultInstance)
                  .build()
              )
            )

          val configurations: ProtoNotification.NotificationConfigurations =
            manager.current(userInfo, AllDeliveryTypes).futureValue
          val chatNewMessageConfigurationOpt =
            configurations.getValuesList.asScala.find(_.getId == ChatNewMessageId)
          chatNewMessageConfigurationOpt.isEmpty shouldBe true
        }
      }
    }

    "merge rent notifications from pushnoy and from notifier" in {
      val passportUser = passportUserGen.next
      val appUser = appUserGen.next
      val userInfo = UserInfo("", 0, Some(appUser.uuid), None, passportUser)

      val watch = getDisabledWatch(passportUser)

      (watches
        .getWatch(_: UserRef)(_: Traced))
        .expects(passportUser, *)
        .returning(Future.successful(watch))
      (pushnoy
        .getDeviceRemovedSubscriptions(_: String)(_: Traced))
        .expects(appUser.uuid, *)
        .returning(Future.successful(Set(RentNotificationsId)))
      (notifier
        .getNotificationSettings(_: PassportUser)(_: Traced))
        .expects(passportUser, *)
        .returning(
          Future.successful(
            InternalGetNotificationSettingsResponse
              .newBuilder()
              .setSuccess(
                NotificationSettings
                  .newBuilder()
                  .addTopics(
                    NotificationTopic
                      .newBuilder()
                      .setTopicId(RentNotificationsId)
                      .setTitle("Яндекс.Аренда")
                      .addChannels(ChannelSettings.newBuilder().setChannel(NotificationChannel.EMAIL).setEnabled(true))
                      .addChannels(ChannelSettings.newBuilder().setChannel(NotificationChannel.SMS).setEnabled(false))
                  )
              )
              .build()
          )
        )

      val configurations: ProtoNotification.NotificationConfigurations =
        withRequestContext(passportUser) { implicit r =>
          manager.current(userInfo, AllDeliveryTypes).futureValue
        }
      val rentConfigurationOpt = configurations.getValuesList.asScala.find(_.getId == RentNotificationsId)
      rentConfigurationOpt.isDefined shouldBe true

      val rentConfiguration = rentConfigurationOpt.get
      rentConfiguration.getTitle shouldBe "Яндекс.Аренда"
      rentConfiguration.getMethodsList.asScala.toSet shouldBe Set(
        DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_PUSH).setEnabled(false).build(),
        DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_SMS).setEnabled(false).build(),
        DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_EMAIL).setEnabled(true).build()
      )
    }

    "update configs in pushnoy and in notifier" in {
      val passportUser = passportUserGen.next
      val appUser = appUserGen.next
      val userInfo = UserInfo("", 0, Some(appUser.uuid), None, passportUser)

      val expectedNotifierRequest = InternalPatchNotificationSettingsRequest
        .newBuilder()
        .addPatch(
          NotificationSettingsPatch
            .newBuilder()
            .setTopicId(RentNotificationsId)
            .setChannel(NotificationChannel.EMAIL)
            .setEnabled(false)
        )
        .addPatch(
          NotificationSettingsPatch
            .newBuilder()
            .setTopicId(RentNotificationsId)
            .setChannel(NotificationChannel.SMS)
            .setEnabled(true)
        )
        .build()

      (pushnoy
        .getDeviceRemovedSubscriptions(_: String)(_: Traced))
        .expects(appUser.uuid, *)
        .returning(Future.successful(Set.empty))
      (pushnoy
        .removeDeviceSubscription(_: String, _: String)(_: Traced))
        .expects(appUser.uuid, ChatNewMessageId, *)
        .returning(Future.unit)
      (notifier
        .patchNotificationSettings(_: PassportUser, _: InternalPatchNotificationSettingsRequest)(_: Traced))
        .expects(passportUser, expectedNotifierRequest, *)
        .returning(
          Future.successful(
            InternalPatchNotificationSettingsResponse
              .newBuilder()
              .setSuccess(
                NotificationSettings
                  .newBuilder()
                  .addTopics(
                    NotificationTopic
                      .newBuilder()
                      .setTopicId(RentNotificationsId)
                      .setTitle(RentNotificationsTitle)
                      .addChannels(ChannelSettings.newBuilder().setEnabled(true).setChannel(NotificationChannel.EMAIL))
                      .addChannels(ChannelSettings.newBuilder().setEnabled(false).setChannel(NotificationChannel.SMS))
                  )
              )
              .build()
          )
        )

      val updateRentNotifications = ProtoNotification.NotificationConfiguration
        .newBuilder()
        .setId(RentNotificationsId)
        .addAllMethods(
          List(
            DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_PUSH).setEnabled(true).build(),
            DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_EMAIL).setEnabled(false).build(),
            DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_SMS).setEnabled(true).build()
          ).asJava
        )
        .build()

      val updateChatNewMessage = ProtoNotification.NotificationConfiguration
        .newBuilder()
        .setId(ChatNewMessageId)
        .addAllMethods(
          List(
            DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_PUSH).setEnabled(false).build()
          ).asJava
        )
        .build()

      val updateRequest = ProtoNotification.NotificationConfigurations
        .newBuilder()
        .addValues(updateRentNotifications)
        .addValues(updateChatNewMessage)
        .build()

      // should not throw
      val result: ProtoNotification.NotificationConfigurations =
        withRequestContext(passportUser) { implicit r =>
          manager.update(userInfo, updateRequest).futureValue
        }

      val rentResult = result.getValuesList.asScala.find(_.getId == RentNotificationsId).get
      rentResult.getTitle shouldBe RentNotificationsTitle
      rentResult.getMethodsList.asScala.toSet shouldBe Set(
        DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_PUSH).setEnabled(true).build(),
        DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_EMAIL).setEnabled(true).build(),
        DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_SMS).setEnabled(false).build()
      )

      val chatNewMessageResult = result.getValuesList.asScala.find(_.getId == ChatNewMessageId).get
      chatNewMessageResult.getTitle shouldBe ChatNewMessageTitle
      chatNewMessageResult.getMethodsList.asScala.toSet shouldBe Set(
        DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_PUSH).setEnabled(false).build()
      )
    }

    "remove delivery types not specified in argument" in {
      val passportUser = passportUserGen.next
      val appUser = appUserGen.next
      val userInfo = UserInfo("", 0, Some(appUser.uuid), None, passportUser)
      val testTopicId = "test_topic"

      val watch = getDisabledWatch(passportUser)

      (watches
        .getWatch(_: UserRef)(_: Traced))
        .expects(passportUser, *)
        .returning(Future.successful(watch))
      (pushnoy
        .getDeviceRemovedSubscriptions(_: String)(_: Traced))
        .expects(appUser.uuid, *)
        .returning(Future.successful(Set(RentNotificationsId)))
      (notifier
        .getNotificationSettings(_: PassportUser)(_: Traced))
        .expects(passportUser, *)
        .returning(
          Future.successful(
            InternalGetNotificationSettingsResponse
              .newBuilder()
              .setSuccess(
                NotificationSettings
                  .newBuilder()
                  .addTopics(
                    NotificationTopic
                      .newBuilder()
                      .setTopicId(RentNotificationsId)
                      .setTitle("Яндекс.Аренда")
                      .addChannels(ChannelSettings.newBuilder().setChannel(NotificationChannel.EMAIL).setEnabled(true))
                      .addChannels(ChannelSettings.newBuilder().setChannel(NotificationChannel.SMS).setEnabled(false))
                  )
                  .addTopics(
                    NotificationTopic
                      .newBuilder()
                      .setTopicId(testTopicId)
                      .setTitle("Тест")
                      .addChannels(ChannelSettings.newBuilder().setChannel(NotificationChannel.SMS).setEnabled(true))
                  )
              )
              .build()
          )
        )

      val configurations: ProtoNotification.NotificationConfigurations =
        withRequestContext(passportUser) { implicit r =>
          manager.current(userInfo, Set(DeliveryTypes.Push, DeliveryTypes.Email)).futureValue
        }

      val rentConfigurationOpt = configurations.getValuesList.asScala.find(_.getId == RentNotificationsId)
      val testConfigurationOpt = configurations.getValuesList.asScala.find(_.getId == testTopicId)

      rentConfigurationOpt.isDefined shouldBe true
      testConfigurationOpt.isDefined shouldBe false

      val rentConfiguration = rentConfigurationOpt.get
      rentConfiguration.getTitle shouldBe "Яндекс.Аренда"
      rentConfiguration.getMethodsList.asScala.toSet shouldBe Set(
        DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_PUSH).setEnabled(false).build(),
        DeliveryMethod.newBuilder().setDeliveryType(DeliveryType.DELIVERY_TYPE_EMAIL).setEnabled(true).build()
      )
    }
  }
}
