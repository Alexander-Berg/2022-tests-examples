package ru.yandex.vos2.autoru.notifications

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsObject, Json}
import ru.auto.api.ApiOfferModel.Category
import ru.auto.notification.palma.proto.PalmaNotificationsTypes.NotificationTopic.MODERATION_UPDATE_STATUS
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.spamalot.SendRequest
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.UserModel.UserPhone
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.letters.ModerationBan
import ru.yandex.vos2.autoru.model.AutoruModelUtils._
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.autoru_api.AutoruApiClient
import ru.yandex.vos2.autoru.services.chat.ChatClient
import ru.yandex.vos2.autoru.utils.letters.PushUtils
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.notifications.{RendererByOffer, _}
import ru.yandex.vos2.services.phone.{SmsSenderClient, SmsTemplateWithParams}
import ru.yandex.vos2.services.pushnoy.{PushTemplate, PushTemplateV1, PushnoyClient}
import ru.yandex.vos2.services.sender.{SenderClient, SenderTemplateWithParams}
import ru.yandex.vos2.services.spamalot.SpamalotClient

import scala.util.Success

/**
  * Created by ndmelentev on 28.02.17.
  */
@RunWith(classOf[JUnitRunner])
class NotifyManagerTest extends AnyWordSpec with Matchers with MockitoSupport with InitTestDbs {

  implicit val trace = Traced.empty

  abstract class Fixture(dealer: Boolean = false) {
    val offer = TestUtils.createOffer(dealer = dealer)
    offer.getUserBuilder.getUserContactsBuilder.setEmail("test@yandex-team.ru")
    offer.getUserBuilder.getUserContactsBuilder.addPhones(UserPhone.newBuilder().setNumber("+79030000000"))

    val emailSender = mockStrict[SenderClient]
    val smsSender = mockStrict[SmsSenderClient]
    val chatClient = mockStrict[ChatClient]
    val pushSender = mockStrict[PushnoyClient]
    val publicApiClient = mockStrict[AutoruApiClient]
    val spamalotClient = mockStrict[SpamalotClient]

    when(emailSender.sendLetter(?, ?)).thenReturn(Success(()))
    when(smsSender.send(?, ?)(?)).thenReturn(Success(""))
    doNothing().when(chatClient).serviceNotification(?, ?, ?)(?)
    doNothing().when(publicApiClient).sendToOfferChat(?, ?, ?, ?, ?, ?, ?, ?)(?)
    when(pushSender.pushToUser(?, ?)(?)).thenReturn(Success(1))
    when(pushSender.hasDevices(?)(?)).thenReturn(true)
    when(spamalotClient.send(?)(?)).thenReturn(Success())

    val isSpamalotFeature: Feature[Boolean] = mock[Feature[Boolean]]
    when(isSpamalotFeature.value).thenReturn(true)

    def notification(hasMail: Boolean = true,
                     hasSms: Boolean = true,
                     hasMessage: Boolean = true,
                     hasPush: Boolean = true): Notification =
      new Notification {
        override val name: String = "Notify1"

        override def mail: Option[SenderTemplateWithParams] =
          if (hasMail) Some(new SenderTemplateWithParams {
            override def payload: JsObject = Json.obj()

            override def name: String = "EmailTemplate"

            override val deliveryParams = offer.toEmailDeliveryParams
          })
          else None

        override def sms: Option[SmsTemplateWithParams] = {
          if (hasSms) Some(new SmsTemplateWithParams {
            override def id: String = "sms_id"

            override def smsText: String = "Sms text"

            override val smsParams = offer.toSmsParams
          })
          else None
        }

        override def push: Option[PushTemplate] = {
          if (hasPush)
            Some(
              PushTemplateV1(
                "low_rating",
                "Промо поднятия в поиске",
                "deeplink",
                Some("android title"),
                Some("ios title"),
                "andorid body",
                "ios body",
                offer.toPushParams
              )
            )
          else None
        }

        override def spamalotPush: Option[SendRequest] = PushUtils.toSpamalotPush(push, MODERATION_UPDATE_STATUS)

        override def chatSupport: Option[ChatSupportTemplate] =
          if (hasMessage)
            Some(
              ChatSupportTemplate(
                user = UserRef.from(offer.getUserRef),
                "Private Message text to tech support room",
                ChatMessageType.ModerationUnban
              )
            )
          else None

        override def offerChat: Option[OfferChatTemplate] = None
        override def notificationCenterChat: Option[NotificationCenterChatTemplate] = None
      }

    val renderer = new RendererByOffer {

      override def render(offer: OfferModel.Offer, extraArgs: Seq[String])(implicit trace: Traced): Notification =
        notification(hasMail = true, hasSms = true, hasMessage = false, hasPush = false)
    }

    val renderers: Map[NotificationType, RendererByOffer] = Map(
      NotificationType.OFFER_EXPIRATION -> renderer
    )

    val userRenderers: Map[NotificationType, RendererByUser] = Map()

    components.featureRegistry.updateFeature(components.featuresManager.SmsNotification.name, true)

    val notifyManager = new AutoruNotifyManager(
      emailSender,
      smsSender,
      chatClient,
      publicApiClient,
      pushSender,
      renderers,
      userRenderers,
      components.featuresManager,
      spamalotClient
    )
  }

  "NotifyManager" should {
    "send" when {
      "all, if have both templates and params" in new Fixture {
        val result = notifyManager.send(notification())
        result shouldEqual NotifySent
      }

      "sms, if hasn't email" in new Fixture {
        offer.getUserBuilder.getUserContactsBuilder.clearEmail()
        val result = notifyManager.send(notification(hasMessage = false, hasPush = false))
        result shouldEqual NotifySent
      }

      "email, if have no phones" in new Fixture {
        offer.getUserBuilder.getUserContactsBuilder.clearPhones()
        val result = notifyManager.send(notification(hasMessage = false, hasPush = false))
        result shouldEqual NotifySent
      }

      "sms, if hasn't mail template" in new Fixture {
        val result = notifyManager.send(notification(hasMail = false, hasMessage = false, hasPush = false))
        result shouldEqual NotifySent
      }

      "email, if hasn't sms template" in new Fixture {
        val result = notifyManager.send(notification(hasSms = false, hasMessage = false, hasPush = false))
        result shouldEqual NotifySent
      }

      "push" in new Fixture {
        val result =
          notifyManager.send(notification(hasMail = false, hasSms = false, hasMessage = false, hasPush = true))
        result shouldEqual NotifySent
      }

      "email, sms and message for moderation ban" in new Fixture {
        val notification = new ModerationBan(
          "offerId",
          Category.CARS,
          "AUDI",
          "A6",
          "http://ya.ru",
          "description",
          Some("smsText"),
          "rules",
          Some("moderation.block_ad"),
          canEdit = false,
          offer.toEmailDeliveryParams,
          offer.toSmsParams,
          Some("Объявление забанено"),
          Some("Объявление забанено"),
          Some("push text"),
          Some("deeplink"),
          offer.toPushParams,
          Some("ban_reason_key"),
          isSpamalotFeature
        )
        val result = notifyManager.send(notification)
        result shouldEqual NotifySent
      }
    }

    "not send" when {
      "if have no delivery params" in new Fixture {
        offer.getUserBuilder.getUserContactsBuilder.clearEmail()
        offer.getUserBuilder.getUserContactsBuilder.clearPhones()

        val result = notifyManager.send(notification(hasMessage = false, hasPush = false))

        result shouldEqual NothingSent
      }

      "if has email, but no email template" in new Fixture {
        offer.getUserBuilder.getUserContactsBuilder.clearPhones()

        val result = notifyManager.send(notification(hasMail = false, hasMessage = false, hasPush = false))

        result shouldEqual NothingSent
      }

      "if has phone, but no sms template" in new Fixture {
        offer.getUserBuilder.getUserContactsBuilder.clearEmail()
        val result = notifyManager.send(notification(hasSms = false, hasMessage = false, hasPush = false))
        result shouldEqual NothingSent
      }

      "if have no templates" in new Fixture {
        val result =
          notifyManager.send(notification(hasMail = false, hasSms = false, hasMessage = false, hasPush = false))
        result shouldEqual NothingSent
      }

      "message if feature is disabled" in new Fixture {
        val messageNotification: Notification =
          notification(hasMail = false, hasSms = false, hasMessage = true, hasPush = false)

        val resultSend: NotifyResult = notifyManager.send(messageNotification)
        val resultSendMessage: NotifyTypeResult = notifyManager.sendMessageToSupportChat(messageNotification)

        resultSend shouldEqual NothingSent
        resultSendMessage shouldBe MessageNothingSent(List("sending messages to tech support chats disabled"))
      }

    }

    "send message to chat instead of octopus" in new Fixture {
      val messageNotification: Notification =
        notification(hasMail = false, hasSms = false, hasMessage = true, hasPush = false)
      val resultSendMessage: NotifyTypeResult = notifyManager.sendMessageToSupportChat(messageNotification)
      resultSendMessage shouldBe MessageNothingSent(List("sending messages to tech support chats disabled"))

      components.featureRegistry.updateFeature(components.featuresManager.MessagesToTechSupport.name, true)
      val resultSendMessage2: NotifyTypeResult = notifyManager.sendMessageToSupportChat(messageNotification)
      resultSendMessage2 shouldBe MessageSent("tech support chat", "user:123")

      verify(chatClient).serviceNotification(any(), any(), any())(?)
      components.featureRegistry.updateFeature(components.featuresManager.MessagesToTechSupport.name, false)
    }

    "send message to dealers" in new Fixture(dealer = true) {
      val messageNotification: Notification =
        notification(hasMail = false, hasSms = false, hasMessage = true, hasPush = false)
      val resultSendMessage: NotifyTypeResult = notifyManager.sendMessageToSupportChat(messageNotification)
      resultSendMessage shouldBe MessageNothingSent(List("sending messages to tech support chats disabled"))

      components.featureRegistry.updateFeature(components.featuresManager.MessagesToTechSupport.name, true)
      val resultSendMessage2: NotifyTypeResult = notifyManager.sendMessageToSupportChat(messageNotification)
      resultSendMessage2 shouldBe MessageSent("tech support chat", "dealer:321")

      verify(chatClient).serviceNotification(any(), any(), any())(?)
      components.featureRegistry.updateFeature(components.featuresManager.MessagesToTechSupport.name, false)
    }
  }

}
