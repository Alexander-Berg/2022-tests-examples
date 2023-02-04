package ru.yandex.vos2.subscriptions.letters

import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.vos2.model.notifications.{Notification, NotificationType}
import ru.yandex.vos2.reasons.{BanReason, BanReasonService}
import ru.yandex.vos2.subscriptions.RealtySubscriptionDefaults._

import scala.collection.JavaConverters._

class RealtyNotificationLetterBuilderSpec extends WordSpecLike with Matchers with Checkers {

  class StaticReasonService(map: Map[Int, BanReason]) extends BanReasonService {

    override def getReason(code: Int): Option[BanReason] =
      map.get(code)
  }

  "Letter builder" should {
    "provide proper template for banned offer" in {
      val errors = Seq(1, 2, 3)
      val service = new StaticReasonService(errors.map(code => code -> reason(code)).toMap)
      val notification = Notification
        .newBuilder()
        .setOfferId("1")
        .setType(NotificationType.NT_OFFER_BANNED)
        .addAllErrors(errors.map(Int.box).asJava)
        .setAddress("address")
        .build()
      val letter = new RealtyNotificationLetterBuilder(service).build(notification)
      letter.name shouldBe "realty.moderation_decline"
    }

    "provide proper template for banned not editable offer" in {
      val errors = Seq(1, 2, 3)
      val service =
        new StaticReasonService(errors.map(code => code -> reason(code, code % 2 == 0)).toMap)
      val notification = Notification
        .newBuilder()
        .setOfferId("1")
        .setType(NotificationType.NT_OFFER_BANNED)
        .addAllErrors(errors.map(Int.box).asJava)
        .setAddress("address")
        .build()
      val letter = new RealtyNotificationLetterBuilder(service).build(notification)
      letter.name shouldBe "realty.moderation_decline_no_edit"
    }

    "provide proper payload for banned offer" in {
      val errors = Seq(1, 2, 3)
      val service = new StaticReasonService(errors.map(code => code -> reason(code)).toMap)
      val notification = Notification
        .newBuilder()
        .setOfferId("1")
        .setType(NotificationType.NT_OFFER_BANNED)
        .addAllErrors(errors.map(Int.box).asJava)
        .setAddress("address")
        .build()
      val letter = new RealtyNotificationLetterBuilder(service).build(notification)
      (letter.payload.value should contain).key("address")
      (letter.payload.value should contain).key("offer_id")
      val reasons = letter.payload.value.get("ban_reason")
      reasons shouldBe defined
      val description = errors.flatMap(service.getReason).flatMap(_.title).mkString("<br/><br/>")
      reasons.get.as[String] shouldBe description
    }

    "handle case of missed reason" in {
      val errors = Seq(1, 2, 3)
      val service =
        new StaticReasonService(errors.map(code => code -> reason(code)).toMap)
      val notification = Notification
        .newBuilder()
        .setOfferId("1")
        .setType(NotificationType.NT_OFFER_BANNED)
        .addAllErrors(errors.map(Int.box).asJava)
        .addErrors(4)
        .setAddress("address")
        .build()
      val letter = new RealtyNotificationLetterBuilder(service).build(notification)
      val reasons = letter.payload.value.get("ban_reason")
      val description = (errors.flatMap(service.getReason).flatMap(_.title) :+
        DefaultDescription).mkString("<br/><br/>")
      reasons.get.as[String] shouldBe description
    }

    "handle case of missed description" in {
      val errors = Seq(1, 2, 3)
      val map = errors.map(code => code -> reason(code)).toMap
      val updated = map + (3 -> reason(3).copy(title = None))
      val service = new StaticReasonService(updated)
      val notification = Notification
        .newBuilder()
        .setOfferId("1")
        .setType(NotificationType.NT_OFFER_BANNED)
        .addAllErrors(errors.map(Int.box).asJava)
        .setAddress("address")
        .build()
      val letter = new RealtyNotificationLetterBuilder(service).build(notification)
      val reasons = letter.payload.value.get("ban_reason")
      val description = (errors.flatMap(service.getReason).flatMap(_.title) :+
        DefaultDescription).mkString("<br/><br/>")
      reasons.get.as[String] shouldBe description
    }
  }

  private def reason(code: Int, isEditable: Boolean = true): BanReason = {
    BanReason(Some(code), Some(s"reason: $code"), None, isEditable)
  }
}
