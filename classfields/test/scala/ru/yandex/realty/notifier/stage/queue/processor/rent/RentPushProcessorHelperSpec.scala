package ru.yandex.realty.notifier.stage.queue.processor.rent

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.sms.SmsSendClient
import ru.yandex.realty.logging.TracedLogging
import ru.yandex.realty.notifier.manager.{NotificationSettingsManager, PushnoyManager, SmsSenderManager}
import ru.yandex.realty.notifier.model.push.rent.OwnerSignContractPush
import ru.yandex.realty.notifier.model.{NotificationTarget, UserTarget}
import ru.yandex.realty.notifier.stage.queue.processor.JustSendNotificationProcessor2
import ru.yandex.realty.pushnoy.model.{PalmaPushInfo, PalmaSmsInfo, Targets}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class RentPushProcessorHelperSpec extends AsyncSpecBase {

  trait Test extends RentPushProcessorHelper with TracedLogging with ExecutionContextProviderFromContext {
    val topicId: Option[String] = Some(NotificationSettingsManager.RentServiceNotificationsTopicId)
    val smsSenderManager: SmsSenderManager = mock[SmsSenderManager]
    val pushnoyManager: PushnoyManager = mock[PushnoyManager]

    val mockSendPush =
      toMockFunction5(
        (pushnoyManager
          .sendPush(_: NotificationTarget, _: PalmaPushInfo, _: Targets.Value, _: Option[String])(_: Traced))
      )

    val mockSendSms =
      toMockFunction5(
        (smsSenderManager
          .sendSms(_: NotificationTarget, _: PalmaSmsInfo, _: SmsSendClient.Templates.Value, _: Option[String])(
            _: Traced
          ))
      )
  }

  private val pushInfo = OwnerSignContractPush(12345, "uyg4587gt4", "8f457yt78")
  private val target = UserTarget(23424)
  implicit private val traced: Traced = Traced.empty

  "RentPushProcessorHelperSpec" when {
    "sendPushOrSmsOrIgnore" should {
      "return Ignore if target None" in new Test {
        sendPushOrSmsOrIgnore(None, target).futureValue should be(JustSendNotificationProcessor2.Ignore)
      }
      "send push and not send sms" in new Test {
        mockSendPush.expects(target, pushInfo, *, topicId, *).returning(Future.successful(1)).once()
        sendPushOrSmsOrIgnore(Some(pushInfo), target).futureValue should be(JustSendNotificationProcessor2.Sent)
      }
      "send sms if send push failed" in new Test {
        mockSendPush.expects(target, pushInfo, *, topicId, *).returning(Future.successful(0)).once()
        mockSendSms.expects(target, pushInfo, *, topicId, *).returning(Future.unit).once()
        sendPushOrSmsOrIgnore(Some(pushInfo), target).futureValue should be(JustSendNotificationProcessor2.Sent)
      }
      "send sms if send push failed with exception" in new Test {
        mockSendPush
          .expects(target, pushInfo, *, topicId, *)
          .returning(Future.failed(new RuntimeException))
          .once()
        mockSendSms.expects(target, pushInfo, *, topicId, *).returning(Future.unit).once()
        sendPushOrSmsOrIgnore(Some(pushInfo), target).futureValue should be(JustSendNotificationProcessor2.Sent)
      }
      "return ignore if send push failed and send sms failed" in new Test {
        mockSendPush
          .expects(target, pushInfo, *, topicId, *)
          .returning(Future.failed(new RuntimeException))
          .once()
        mockSendSms
          .expects(target, pushInfo, *, topicId, *)
          .returning(Future.failed(new RuntimeException))
          .once()
        sendPushOrSmsOrIgnore(Some(pushInfo), target).futureValue should be(JustSendNotificationProcessor2.Ignore)
      }
    }
  }
}
