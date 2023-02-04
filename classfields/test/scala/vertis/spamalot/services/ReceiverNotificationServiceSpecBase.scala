package vertis.spamalot.services

import cats.syntax.option._
import com.google.protobuf.timestamp.Timestamp
import org.scalactic.{Equality, TypeCheckedTripleEquals}
import org.scalatest.{EitherValues, OptionValues}
import ru.yandex.vertis.common.Domain.DOMAIN_AUTO
import ru.yandex.vertis.spamalot.inner.StoredNotification
import ru.yandex.vertis.spamalot.model.{ReceiverId => ProtoReceiverId}
import vertis.core.utils.NoWarnFilters
import vertis.spamalot.inner.StoredNotificationValidators
import vertis.spamalot.mocks.TestSendingTimeService
import vertis.spamalot.services.impl.ReceiverNotificationServiceImpl
import vertis.spamalot.{SpamalotOptics, SpamalotYdbTest}
import vertis.zio.test.ZioSpecBase
import vertistraf.common.pushnoy.client.mocks.TestPushnoyClient

trait ReceiverNotificationServiceSpecBase
  extends ZioSpecBase
  with SpamalotYdbTest
  with TypeCheckedTripleEquals
  with SpamalotOptics
  with OptionValues
  with EitherValues {

  protected val timestampEquality: Equality[Timestamp] = new Equality[Timestamp] {

    override def areEqual(left: Timestamp, other: Any): Boolean = other match {
      case right: Timestamp =>
        left.seconds === right.seconds &&
        (math.abs(left.nanos - right.nanos) <= 1000)
      case _ => Equality.default.areEqual(left, other)
    }
  }

  protected val insertedStoredNotificationEquality: Equality[StoredNotification] = new Equality[StoredNotification] {
    implicit private val tsEquality: Equality[Timestamp] = timestampEquality

    @annotation.nowarn(NoWarnFilters.Deprecation)
    private def areReceiversEqual(left: StoredNotification, right: StoredNotification): Boolean =
      (left.receiverId, right.receiverId) match {
        case (None, None) => left.userId === right.userId
        case _ =>
          (
            left.receiverId === right.receiverId
          ) ||
          (left.receiverId === ProtoReceiverId(ProtoReceiverId.Id.UserId(right.userId)).some) ||
          (ProtoReceiverId(ProtoReceiverId.Id.UserId(left.userId)).some === right.receiverId)
      }

    override def areEqual(left: StoredNotification, other: Any): Boolean = other match {
      case right: StoredNotification =>
        left.id === right.id &&
        areReceiversEqual(left, right) &&
        left.topic === right.topic &&
        left.createTs === right.createTs &&
        left.name === right.name &&
        left.payload === right.payload &&
        left.notificationObject === right.notificationObject &&
        left.isRead === right.isRead &&
        left.unknownFields === right.unknownFields
      case _ => Equality.default.areEqual(left, other)
    }
  }

  protected lazy val pushClient = new TestPushnoyClient(DOMAIN_AUTO)

  protected lazy val service =
    new ReceiverNotificationServiceImpl(
      DOMAIN_AUTO,
      storages,
      receiverSettingsService,
      testBrokerService,
      pushClient,
      TestSendingTimeService,
      StoredNotificationValidators.default
    )

  protected val implement: AfterWord = afterWord("implement")

  protected val nonExistentNotificationId: String = "non_existent_notification_id"
}
