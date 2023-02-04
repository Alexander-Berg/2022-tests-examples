package ru.yandex.auto.garage.scheduler.stage.notifications

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{Insurance, Notification}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Notification.NotificationType
import ru.yandex.vertis.util.time.DateTimeUtil

import java.time.{LocalDateTime, ZoneOffset}

object NotificationsTestUtils {

  def notificationTemplate(
      notificationType: NotificationType = NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION,
      deadline: Timestamp = Timestamps.fromSeconds(LocalDateTime.now().plusDays(1).toEpochSecond(ZoneOffset.UTC)),
      numTries: Int = 3,
      sent: Option[Timestamp] = None,
      notSent: Option[Timestamp] = None): Notification = {

    val builder = Notification
      .newBuilder()
      .setNotificationType(notificationType)
      .setNumTries(numTries)
      .setDeliveryDeadline(deadline)
      .setMaxTries(5)

    sent.foreach(builder.setTimestampSent)
    notSent.foreach(builder.setTimestampCancel)

    builder.build()
  }

  def buildInsurancePolicy(
      insuranceType: InsuranceType = InsuranceType.OSAGO,
      isDeleted: Boolean = false,
      policyTo: DateTime = DateTimeUtil.now()): Insurance = {
    Insurance
      .newBuilder()
      .setInsuranceType(insuranceType)
      .setIsDeleted(isDeleted)
      .setTo(Timestamps.fromMillis(policyTo.getMillis))
      .build()
  }
}
