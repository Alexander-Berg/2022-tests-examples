package ru.yandex.vertis.safe_deal.notification

import cats.implicits._
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.safe_deal.dictionary.NotificationTemplateDictionary
import ru.yandex.vertis.safe_deal.model.{Tag, _}
import ru.yandex.vertis.safe_deal.model.ChatNotification.{ByTemplateContent => ChatByTemplateContent}
import ru.yandex.vertis.safe_deal.model.event.DealChangedEvent.Event
import ru.yandex.vertis.safe_deal.model.NotificationTemplate.ChatContent
import ru.yandex.vertis.safe_deal.notification
import ru.yandex.vertis.safe_deal.proto.common.ParticipantType
import ru.yandex.vertis.safe_deal.proto.{model => proto}
import ru.yandex.vertis.safe_deal.util.mock.NotificationTemplateDictionaryMock
import ru.yandex.vertis.safe_deal.util.RichAutoruModel._
import ru.yandex.vertis.zio_baker.model.User
import ru.yandex.vertis.zio_baker.util.EmptyString
import zio._
import zio.test._
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object NotificationCreatorSpec extends DefaultRunnableSpec {

  private lazy val notificationId: NotificationId = UUID.randomUUID.toString.taggedWith[Tag.NotificationId]
  private val domain: Domain = Domain.DOMAIN_AUTO

  private lazy val ts: Instant = Instant.now

  private lazy val notificationTemplateDictionaryLayer: ULayer[NotificationTemplateDictionary] =
    ZLayer.succeed(new NotificationTemplateDictionaryMock())

  private lazy val notificationCreatorLayer: ULayer[NotificationCreator] =
    notificationTemplateDictionaryLayer >>> NotificationCreator.live

  sealed trait NotificationCreatorTestCase {

    def description: String
    def expected: Seq[Notification]
  }

  case class CreateByArgumentsTestCase(
      description: String,
      deal: Deal,
      participantType: ParticipantType,
      template: String,
      ts: Instant,
      idempotencyPeriod: FiniteDuration,
      expected: Seq[Notification])
    extends NotificationCreatorTestCase

  object CreateByArgumentsTestCase {

    val deal: Deal = Arbitraries.AutoruDealArb.arbitrary.sample.get
    val participantType: ParticipantType = ParticipantType.BUYER
    val template: String = "sent-request"
    val idempotencyPeriod: FiniteDuration = 1.day
    val recipient: User = deal.getUserByParticipantType(participantType).get.get
    val notificationTemplateId: NotificationTemplateId = s"buyer-$template-chat".taggedWith[Tag.NotificationTemplateId]
    val params: Map[String, String] = deal.getParamsByParticipantType(participantType)
    val later: Instant = ts.plusSeconds(24L * 60L * 60L + 1L)

    def idempotencyKey(ts: Instant): Option[String] =
      notification.idempotencyKey(notificationTemplateId, ts, idempotencyPeriod.some).some

    val chatNotificationTestCase: ChatNotificationTestCase =
      ChatNotificationTestCase(
        recipient = recipient,
        idempotencyKey = idempotencyKey(ts),
        notificationTemplateId = notificationTemplateId,
        params = params
      )
  }

  case class CreateByDealChangedEventTestCase(
      description: String,
      deal: Deal,
      event: Event,
      ts: Instant,
      expected: Seq[Notification])
    extends NotificationCreatorTestCase

  object CreateByDealChangedEventTestCase {

    val deal: Deal = Arbitraries.AutoruDealArb.arbitrary.sample.get
    val participantType: ParticipantType = ParticipantType.SELLER
    val idempotencyPeriod: FiniteDuration = 5.minutes
    val recipient: User = deal.getUserByParticipantType(participantType.another).get.get

    val notificationTemplateId: NotificationTemplateId =
      s"seller-edited-credentials-chat".taggedWith[Tag.NotificationTemplateId]
    val params: Map[String, String] = deal.getParamsByParticipantType(participantType)

    def idempotencyKey(ts: Instant): Option[String] =
      notification.idempotencyKey(notificationTemplateId, ts, idempotencyPeriod.some).some

    val chatNotificationTestCase: ChatNotificationTestCase =
      ChatNotificationTestCase(
        recipient = recipient,
        idempotencyKey = idempotencyKey(ts),
        notificationTemplateId = notificationTemplateId,
        params = params
      )
  }

  private val notificationCreatorTestCases: Seq[NotificationCreatorTestCase] = Seq(
    {
      import CreateByArgumentsTestCase._
      CreateByArgumentsTestCase(
        description = "create by arguments – idempotent notification doesn't exists",
        deal = deal,
        participantType = participantType,
        template = template,
        ts = ts,
        idempotencyPeriod = idempotencyPeriod,
        expected = Seq(chatNotificationTestCase.chatNotification)
      )
    }, {
      import CreateByArgumentsTestCase._
      CreateByArgumentsTestCase(
        description = "create by arguments – idempotent notification exists",
        deal = deal.addNotifications(
          notifications = Seq(chatNotificationTestCase.chatNotification),
          updated = ts
        ),
        participantType = participantType,
        template = template,
        ts = ts,
        idempotencyPeriod = idempotencyPeriod,
        expected = Seq.empty
      )
    }, {
      import CreateByArgumentsTestCase._
      CreateByArgumentsTestCase(
        description = "create by arguments – idempotent notification exists (period expired)",
        deal = deal.addNotifications(
          notifications = Seq(chatNotificationTestCase.chatNotification),
          updated = ts
        ),
        participantType = participantType,
        template = template,
        ts = later,
        idempotencyPeriod = idempotencyPeriod,
        expected = Seq(
          chatNotificationTestCase
            .copy(ts = later, idempotencyKey = idempotencyKey(later))
            .chatNotification
        )
      )
    }, {
      import CreateByDealChangedEventTestCase._
      CreateByDealChangedEventTestCase(
        description = "create by deal changed event – idempotent notification doesn't exists",
        deal = deal,
        event = Event.Party.Seller.EditedCredentials,
        ts = ts,
        expected = Seq(chatNotificationTestCase.chatNotification)
      )
    }, {
      import CreateByDealChangedEventTestCase._
      CreateByDealChangedEventTestCase(
        description = "create by deal changed event – idempotent notification exists",
        deal = deal.addNotifications(
          notifications = Seq(chatNotificationTestCase.chatNotification),
          updated = ts
        ),
        event = Event.Party.Seller.EditedCredentials,
        ts = ts,
        expected = Seq.empty
      )
    }
  )

  private val notificationCreatorTests = notificationCreatorTestCases.map { testCase =>
    val notifications = testCase match {
      case CreateByArgumentsTestCase(_, deal, participantType, template, ts, idempotencyPeriod, _) =>
        NotificationCreator.create(deal, participantType, template, ts, idempotencyPeriod)
      case CreateByDealChangedEventTestCase(_, deal, event, ts, _) =>
        NotificationCreator.create(deal, event, ts)
    }
    val significant = notifications.map(_.map(_.significant))
    testM(testCase.description) {
      assertM(significant)(equalTo(testCase.expected)).provideLayer(notificationCreatorLayer)
    }
  }

  case class ChatNotificationTestCase(
      ts: Instant = ts,
      recipient: User,
      idempotencyKey: Option[String],
      notificationTemplateId: NotificationTemplateId,
      params: Map[String, String]) {

    def chatNotification: ChatNotification =
      ChatNotification(
        id = notificationId,
        created = ts,
        updated = ts,
        state = proto.Notification.State.NEW,
        recipient = recipient,
        idempotencyKey = idempotencyKey,
        content = ChatByTemplateContent(
          template = NotificationTemplate(
            id = notificationTemplateId,
            domain = domain,
            content = ChatContent(
              body = EmptyString
            )
          ),
          params = params
        )
      )
  }

  implicit class RichNotification(val value: Notification) extends AnyVal {

    def significant: Notification = value match {
      case n: PushNotification => n.copy(id = notificationId)
      case n: ChatNotification => n.copy(id = notificationId)
      case n: EmailNotification => n.copy(id = notificationId)
      case n: SmsNotification => n.copy(id = notificationId)
    }
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("NotificationCreator")(notificationCreatorTests: _*)
}
