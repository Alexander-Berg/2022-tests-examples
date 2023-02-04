package ru.yandex.realty.notifier.manager.log

import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doAnswer
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.{Json, OWrites}
import realty.palma.PushTemplateOuterClass.PushTemplate
import ru.yandex.realty.notification.event.Event.NotificationEvent
import ru.yandex.realty.notifier.model.{DeviceTarget, EmailTarget, NotificationTarget, UserTarget}
import ru.yandex.realty.notifier.model.email.{CallComplaintEmailPayload, CallComplaintEmailPayloadPrimary}
import ru.yandex.realty.notifier.model.email.CallComplaintEmailPayload.asJsObject
import ru.yandex.realty.notifier.model.push.{SimilarOffersReminderPush, SocialBindReminderPush}
import ru.yandex.realty.notifier.model.push.SimilarOffersReminderPush.SimilarOffersVarText
import ru.yandex.realty.pushnoy.model.{MetrikaPushId, PalmaPushInfoV3, PalmaPushMessage, PushInfoV3}
import ru.yandex.realty.pushnoy.model.PalmaPushId.{SIMILAR_OFFERS_REMINDER_FLAT, SOCIAL_BIND_REMINDER}
import ru.yandex.realty.pushnoy.model.PushId.{SimilarSitesPush, SocialBindReminder}
import ru.yandex.realty.sender.model.{EmailSenderResponse, SendTo, SendToEmail, SendToUid}
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class NotificationLogServiceSpec
  extends WordSpec
  with Matchers
  with MockitoSupport
  with ScalaFutures
  with PropertyChecks {

  implicit private val ec: ExecutionContext = ExecutionContext.global
  implicit private val Writes: OWrites[CallComplaintEmailPayload] = payload => asJsObject(payload)
  implicit private val protoMarshaller: ProtoMarshaller[NotificationEvent] = ProtoMarshaller.google[NotificationEvent]

  private val emailTestData =
    Table(
      ("sendTo", "templateId", "payload", "error", "expected"),
      (
        SendToEmail("my@mail.xyz"),
        "myTemplate",
        CallComplaintEmailPayloadPrimary(
          "date",
          "time",
          "from",
          "to",
          "site"
        ),
        None,
        EmailTarget("my@mail.xyz").toPlain
      ),
      (
        SendToUid(12341L),
        "templateId0",
        CallComplaintEmailPayloadPrimary(
          "someDate",
          "timeAfterTime",
          "fromPhone",
          "toPhone",
          "site Description"
        ),
        Option("error desc"),
        UserTarget(12341L).toPlain
      )
    )

  val configuration: Configuration = prepareConfiguration
  private val pushTestData =
    Table(
      ("target", "pushInfo", "palmaPushMessage", "expected"),
      (
        DeviceTarget("deviceId"),
        SimilarOffersReminderPush(
          SimilarOffersVarText("21 вариант", "похожий"),
          SIMILAR_OFFERS_REMINDER_FLAT,
          "offerId12314"
        ),
        PalmaPushMessage(
          SIMILAR_OFFERS_REMINDER_FLAT,
          "1",
          "2",
          Some(MetrikaPushId.SimilarSearch),
          SimilarSitesPush,
          configuration,
          PushTemplate.PushState.SEND_N_LOG
        ),
        DeviceTarget("deviceId").toPlain
      ),
      (
        UserTarget(123L),
        SocialBindReminderPush("socialUid"),
        PalmaPushMessage(
          SOCIAL_BIND_REMINDER,
          "1",
          "2",
          Some(MetrikaPushId.SocialBindReminder),
          SocialBindReminder,
          configuration,
          PushTemplate.PushState.ONLY_LOG
        ),
        UserTarget(123L).toPlain
      )
    )

  "LogSentEmailsService" should {

    forAll(emailTestData) {
      (
        sendTo: SendTo,
        templateId: String,
        payload: CallComplaintEmailPayload,
        error: Option[String],
        expected: String
      ) =>
        "logEmail for " + expected in new NotificationLogServiceFixture {
          service
            .logEmail(
              sendTo,
              templateId,
              payload,
              EmailSenderResponse(error.isEmpty, error)
            )

          sendMessage.getValue.getTarget shouldBe expected
          sendMessage.getValue.getEmailPayload.getTemplateId shouldBe templateId
          sendMessage.getValue.getEmailPayload.getPayload shouldBe Json.toJson(payload).toString()
          sendMessage.getValue.getEmailPayload.getResponse.getSuccess shouldBe error.isEmpty
          sendMessage.getValue.getEmailPayload.getResponse.hasError shouldBe error.isDefined
          if (error.isDefined) {
            sendMessage.getValue.getEmailPayload.getResponse.getError.getValue shouldBe error.get
          }
        }
    }

    forAll(pushTestData) {
      (
        target: NotificationTarget,
        palmaPushInfo: PalmaPushInfoV3,
        palmaPushMessage: PalmaPushMessage,
        expected: String
      ) =>
        "logPush for " + expected in new NotificationLogServiceFixture {
          val pushInfo: PushInfoV3 = palmaPushInfo.renderWith(palmaPushMessage)

          service.logPush(pushInfo, palmaPushMessage, target)

          sendMessage.getValue.getTarget shouldBe expected
          sendMessage.getValue.getPushPayload.getAndroidMinVersion shouldBe palmaPushMessage.androidMinVersion
          sendMessage.getValue.getPushPayload.getIosMinVersion shouldBe palmaPushMessage.iosMinVersion
          sendMessage.getValue.getPushPayload.getMetrikaId shouldBe palmaPushMessage.metrikaId.get.toString
          sendMessage.getValue.getPushPayload.getTitle shouldBe (if (palmaPushMessage.palmaPushId == SIMILAR_OFFERS_REMINDER_FLAT)
                                                                   "title1"
                                                                 else "title2")
          sendMessage.getValue.getPushPayload.getText shouldBe (if (palmaPushMessage.palmaPushId == SIMILAR_OFFERS_REMINDER_FLAT)
                                                                  "text1"
                                                                else "text2")
        }
    }
  }

  trait NotificationLogServiceFixture {
    val brokerClient: BrokerClient = mock[BrokerClient]
    val service = new NotificationLogService(brokerClient)

    val sendMessage: ArgumentCaptor[NotificationEvent] =
      ArgumentCaptor.forClass[NotificationEvent, NotificationEvent](classOf[NotificationEvent])
    doAnswer(new Answer[Future[Unit]]() {

      import org.mockito.invocation.InvocationOnMock

      def answer(invocation: InvocationOnMock): Future[Unit] = {
        Future.successful()
      }
    }).when(brokerClient).send(any(), sendMessage.capture())(any())
  }

  private def prepareConfiguration = {
    val templateLoader = new StringTemplateLoader()
    templateLoader.putTemplate("SIMILAR_OFFERS_REMINDER_FLAT/title", "title1")
    templateLoader.putTemplate("SIMILAR_OFFERS_REMINDER_FLAT/text", "text1")
    templateLoader.putTemplate("SOCIAL_BIND_REMINDER/title", "title2")
    templateLoader.putTemplate("SOCIAL_BIND_REMINDER/text", "text2")
    val configuration = new Configuration(Configuration.VERSION_2_3_28)
    configuration.setTemplateLoader(templateLoader)
    configuration
  }

}
