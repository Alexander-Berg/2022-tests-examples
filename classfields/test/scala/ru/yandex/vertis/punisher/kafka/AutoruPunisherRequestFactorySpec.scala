package ru.yandex.vertis.punisher.kafka

import java.time.Instant

import cats.data.NonEmptyList
import fs2.kafka
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel
import ru.yandex.vertis.events.{OfferCreate, _}
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.Generators._
import ru.yandex.vertis.punisher.kafka.AutoruPunisherRequestFactorySpec._
import ru.yandex.vertis.punisher.model.PunisherRequest.UserCheck
import ru.yandex.vertis.punisher.model.{KafkaOffset, TaskDomain, TaskDomainImpl}
import ru.yandex.vertis.punisher.util.DateTimeUtils._
import ru.yandex.vertis.{Domain, RequestContext}

import scala.concurrent.duration._

//noinspection TypeAnnotation
@RunWith(classOf[JUnitRunner])
class AutoruPunisherRequestFactorySpec extends BaseSpec {
  val userId = "1"

  val kafkaOffset = generate[KafkaOffset]
  val noneTimestamp = kafka.Timestamp.none

  val offerId = "offerId"
  val autoruOffer = buildAutoruOffer(offerId)

  val offerCreateEvent = buildOfferCreateEvent(autoruOffer)
  val offerUpdateEvent = buildOfferUpdateEvent(autoruOffer)

  val userEvent = UserEvent.newBuilder.build

  val autoOffers = TaskDomainImpl(Domain.DOMAIN_AUTO, TaskDomain.Labels.Offers)
  val autoInfection = TaskDomainImpl(Domain.DOMAIN_AUTO, TaskDomain.Labels.Infection)
  val autoPhone = TaskDomainImpl(Domain.DOMAIN_AUTO, TaskDomain.Labels.Phone)
  val autoDeviceCheck = TaskDomainImpl(Domain.DOMAIN_AUTO, TaskDomain.Labels.DeviceCheck)

  "PunisherRequestFactory" should {
    "buildPunisherRequest from offer create event" in {
      val eventWithOfferCreateEvent = buildEvent(offerCreateEvent)

      val expected =
        Some(UserCheck(userId, NonEmptyList.of(autoOffers, autoInfection, autoPhone), Some(offerId), Some(kafkaOffset)))
      AutoruPunisherRequestFactory
        .apply(userId, eventWithOfferCreateEvent, kafkaOffset, None)
        .map(_.check) shouldBe expected
    }

    "buildPunisherRequest for auto-offers from the fresh offer create event" in {
      val offerCreateTimestamp = generate[Instant]
      val recordTimestamp = offerCreateTimestamp.plus(7.days)

      val offerWithCreateTime = buildAutoruOffer(offerId, createTime = Some(offerCreateTimestamp))
      val offerCreateEvent = buildOfferCreateEvent(offerWithCreateTime)
      val eventWithOfferCreateEvent = buildEvent(offerCreateEvent)

      val request =
        AutoruPunisherRequestFactory.apply(userId, eventWithOfferCreateEvent, kafkaOffset, Some(recordTimestamp)).get
      request.check.asInstanceOf[UserCheck].taskDomains.toList should contain(autoOffers)
    }

    "not buildPunisherRequest for auto-offers from the old offer create event" in {
      val offerCreateTimestamp = generate[Instant]
      val recordTimestamp = offerCreateTimestamp.plus(60.days)

      val offerWithCreateTime = buildAutoruOffer(offerId, createTime = Some(offerCreateTimestamp))
      val offerCreateEvent = buildOfferCreateEvent(offerWithCreateTime)
      val eventWithOfferCreateEvent = buildEvent(offerCreateEvent)

      val request =
        AutoruPunisherRequestFactory.apply(userId, eventWithOfferCreateEvent, kafkaOffset, Some(recordTimestamp)).get
      request.check.asInstanceOf[UserCheck].taskDomains.toList should not contain autoOffers
    }

    "buildPunisherRequest from offer update event" in {
      val eventWithOfferUpdateEvent = buildEvent(offerUpdateEvent)

      val expected =
        Some(UserCheck(userId, NonEmptyList.of(autoInfection, autoPhone), Some(offerId), Some(kafkaOffset)))
      AutoruPunisherRequestFactory
        .apply(userId, eventWithOfferUpdateEvent, kafkaOffset, None)
        .map(_.check) shouldBe expected
    }

    "buildPunisherRequest from user event" in {
      val eventWithUserEvent =
        Event
          .newBuilder()
          .setUserEvent(userEvent)
          .setRequestContext(RequestContext.newBuilder())
          .build()

      val expected = Some(UserCheck(userId, NonEmptyList.of(autoInfection, autoPhone), None, Some(kafkaOffset)))
      AutoruPunisherRequestFactory.apply(userId, eventWithUserEvent, kafkaOffset, None).map(_.check) shouldBe expected
    }
  }
}

object AutoruPunisherRequestFactorySpec {
  private def buildAutoruOffer(id: String, createTime: Option[Instant] = None): ApiOfferModel.Offer = {
    val builder = ru.auto.api.ApiOfferModel.Offer.newBuilder().setId(id)
    createTime.map { instant =>
      val additionalInfo = ru.auto.api.ApiOfferModel.AdditionalInfo.newBuilder().setCreationDate(instant.toEpochMilli)
      builder.setAdditionalInfo(additionalInfo)
    }
    builder.build
  }

  private def buildOfferCreateEvent(offer: ApiOfferModel.Offer): OfferEvent = {
    val offerCreate = OfferCreate.newBuilder().setOffer(Offer.newBuilder().setAuto(offer))
    OfferEvent
      .newBuilder()
      .setCreate(offerCreate)
      .build
  }

  private def buildOfferUpdateEvent(offer: ApiOfferModel.Offer): OfferEvent = {
    val offerUpdate = OfferUpdate.newBuilder().setOffer(Offer.newBuilder().setAuto(offer))
    OfferEvent
      .newBuilder()
      .setUpdate(offerUpdate)
      .build
  }

  private def buildEvent(offerEvent: OfferEvent) =
    Event
      .newBuilder()
      .setOfferEvent(offerEvent)
      .setRequestContext(RequestContext.newBuilder())
      .build()
}
