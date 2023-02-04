package ru.yandex.vertis.punisher.kafka

import cats.effect.Sync
import fs2.kafka
import fs2.kafka.ConsumerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.dao.AutoruUserLastYandexUidTsDao
import ru.yandex.vertis.punisher.model.{PunisherRequest, TaskId, UserId}
import ru.yandex.vertis.quality.kafka_utils.KafkaProducer
import ru.yandex.vertis.{events, Domain, RequestContext}
import org.mockito.Mockito._
import ru.yandex.vertis.events.{Event, Offer, OfferCreate, OfferEvent}
import ru.yandex.vertis.punisher.kafka.actions.VertisEventLogConsumerActions
import ru.yandex.vertis.quality.cats_utils.Awaitable._

@RunWith(classOf[JUnitRunner])
class VertisEventLogConsumerActionsSpec extends BaseSpec {
  val dao = mock[AutoruUserLastYandexUidTsDao[F]]
  when(dao.upsertNewerTsOrIgnore(any(), any())).thenReturn(Sync[F].unit)
  val kafkaProducer = mock[KafkaProducer[F, UserId, PunisherRequest]]
  val recordMetadata = null.asInstanceOf[RecordMetadata]
  when(kafkaProducer.append(any())).thenReturn(Sync[F].pure(recordMetadata))

  val consumerActions = new VertisEventLogConsumerActions(dao, AutoruPunisherRequestFactory, kafkaProducer)

  before {
    clearInvocations(dao)
    clearInvocations(kafkaProducer)
  }

  "VertisEventLogConsumerActions" should {
    val consumerRecord = mock[ConsumerRecord[Option[String], Event]]

    val userId = Some("userId")
    val autoDomain = Some(Domain.DOMAIN_AUTO)
    val realtyDomain = Some(Domain.DOMAIN_REALTY)
    val yandexUid = Some("12345678901234567890")

    "accept auto events with userId" in {
      when(consumerRecord.value).thenReturn(buildEvent(userId = userId, domain = autoDomain))
      consumerActions.accept(consumerRecord) shouldBe true
    }

    "deny realty events with userId" in {
      when(consumerRecord.value).thenReturn(buildEvent(userId = userId, domain = realtyDomain))
      consumerActions.accept(consumerRecord) shouldBe false
    }

    "deny events without userId field" in {
      when(consumerRecord.value).thenReturn(buildEvent(domain = autoDomain))
      consumerActions.accept(consumerRecord) shouldBe false
    }

    "deny events without domain field" in {
      when(consumerRecord.value).thenReturn(buildEvent(userId = userId))
      consumerActions.accept(consumerRecord) shouldBe false
    }

    "upsert ts and produce tasks if fields are correct" in {
      val event = buildEvent(userId = userId, domain = autoDomain, yandexUid = yandexUid, setEvent = true)
      when(consumerRecord.value).thenReturn(event)
      when(consumerRecord.timestamp).thenReturn(kafka.Timestamp.none)
      consumerActions.consume(consumerRecord).await
      verify(dao).upsertNewerTsOrIgnore(any(), any())
      verify(kafkaProducer).append(any())
    }

    "do nothing if event model hasn't any events" in {
      val event = buildEvent(userId = userId, domain = autoDomain, yandexUid = yandexUid)
      when(consumerRecord.value).thenReturn(event)
      when(consumerRecord.timestamp).thenReturn(kafka.Timestamp.none)
      consumerActions.consume(consumerRecord).await
      verifyNoInteractions(dao)
      verifyNoInteractions(kafkaProducer)
    }
  }

  private def buildEvent(userId: Option[TaskId] = None,
                         domain: Option[Domain] = None,
                         yandexUid: Option[TaskId] = None,
                         setEvent: Boolean = false
                        ) = {
    val rcBuilder = RequestContext.newBuilder()
    userId.foreach(rcBuilder.setUserId)
    yandexUid.foreach(rcBuilder.setYandexUid)
    domain.foreach(rcBuilder.setDomain)

    val autoruOffer = ru.auto.api.ApiOfferModel.Offer.newBuilder()
    val offer = Offer.newBuilder().setAuto(autoruOffer)
    val offerCreate = OfferCreate.newBuilder().setOffer(offer)
    val offerEvent = OfferEvent.newBuilder().setCreate(offerCreate)

    val eventBuilder = events.Event.newBuilder().setRequestContext(rcBuilder)

    if (setEvent) eventBuilder.setOfferEvent(offerEvent)

    eventBuilder.build()
  }
}
