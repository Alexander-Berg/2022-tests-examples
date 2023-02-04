package ru.yandex.vertis.statistics.core.converters

import com.google.protobuf.{Int64Value, Timestamp}
import org.joda.time.Instant
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.events.{Event, EventTypeNamespace}
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.proto.offer._
import ru.yandex.realty.statistics.model.RealtySiteEvent

import java.util.{Collections, UUID}
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class SiteEventConverterSpec extends SpecBase with PropertyChecks {

  private val eventId: String = UUID.randomUUID().toString
  private val siteId: String = Gen.choose(1, Long.MaxValue).sample.get.toString
  private val offerId: String = Gen.choose(1, Long.MaxValue).sample.get.toString
  private val partnerId: String = Gen.choose(1, Long.MaxValue).sample.get.toString
  private val campaignId: String = Gen.choose(1, Long.MaxValue).sample.get.toString
  private val companyId: Long = Gen.choose(1, Long.MaxValue).sample.get
  private val agencyId: Long = Gen.choose(1, Long.MaxValue).sample.get
  private val clientId: Long = Gen.choose(1, Long.MaxValue).sample.get
  private val timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000)
  private val eventTestData =
    Table(
      ("description", "event", "campaignOpt", "siteEventOpt"),
      ("with no data", Event.getDefaultInstance, None, None),
      ("with no event type", {
        val builder = Event.newBuilder()
        builder.setEventId(eventId)
        builder.setTimestamp(timestamp)
        builder.getObjectInfoBuilder.getSiteInfoBuilder.setSiteId(siteId)
        builder.build()
      }, None, None),
      ("with skipped event type", {
        val builder = Event.newBuilder()
        builder.setEventId(eventId)
        builder.setTimestamp(timestamp)
        builder.getObjectInfoBuilder.getSiteInfoBuilder.setSiteId(siteId)
        builder.setEventType(EventTypeNamespace.EventType.OFFER_CLICK)
        builder.build()
      }, None, None),
      (
        "with site click data", {
          val builder = Event.newBuilder()
          builder.setEventId(eventId)
          builder.setTimestamp(timestamp)
          builder.getObjectInfoBuilder.getSiteInfoBuilder.setSiteId(siteId)
          builder.setEventType(EventTypeNamespace.EventType.CARD_SHOW)
          builder.build()
        },
        None,
        Some(
          RealtySiteEvent
            .newBuilder()
            .setSiteId(siteId.toLong)
            .setEventId(eventId)
            .setTimestamp(timestamp.build())
            .setStatType(RealtySiteEvent.StatType.SITE_CLICK)
            .build()
        )
      ),
      ("with no offer type and category", {
        val builder = Event.newBuilder()
        builder.setEventId(eventId)
        builder.setTimestamp(timestamp)
        val objectInfoBuilder = builder.getObjectInfoBuilder
        objectInfoBuilder.getSiteInfoBuilder.setSiteId(siteId)
        objectInfoBuilder.getOfferInfoBuilder.setOfferId(offerId).setPartnerId(partnerId)
        builder.setEventType(EventTypeNamespace.EventType.OFFER_SHOW)
        builder.build()
      }, None, None),
      (
        "with offer show data", {
          val builder = Event.newBuilder()
          builder.setEventId(eventId)
          builder.setTimestamp(timestamp)
          val objectInfoBuilder = builder.getObjectInfoBuilder
          objectInfoBuilder.getSiteInfoBuilder.setSiteId(siteId)
          objectInfoBuilder.getOfferInfoBuilder
            .setOfferId(offerId)
            .setPartnerId(partnerId)
            .setOfferTypeField(OfferType.SELL)
            .setOfferCategoryField(OfferCategory.APARTMENT)
            .getSellOfferBuilder
            .setPrimarySale(true)
          builder.setEventType(EventTypeNamespace.EventType.OFFER_SHOW)
          builder.build()
        },
        None,
        Some(
          RealtySiteEvent
            .newBuilder()
            .setEventId(eventId)
            .setTimestamp(timestamp.build())
            .setSiteId(siteId.toLong)
            .setStatType(RealtySiteEvent.StatType.OFFER_SHOW)
            .build()
        )
      ),
      (
        "with campaign data", {
          val builder = Event.newBuilder()
          builder.setEventId(eventId)
          builder.setTimestamp(timestamp)
          val objectInfoBuilder = builder.getObjectInfoBuilder
          objectInfoBuilder.getSiteInfoBuilder.setSiteId(siteId)
          objectInfoBuilder.getOfferInfoBuilder
            .setOfferId(offerId)
            .setPartnerId(partnerId)
            .setOfferTypeField(OfferType.SELL)
            .setOfferCategoryField(OfferCategory.APARTMENT)
            .getSellOfferBuilder
            .setPrimarySale(true)
          builder.setEventType(EventTypeNamespace.EventType.OFFER_SHOW)
          builder.build()
        },
        Some(
          new Campaign(
            campaignId,
            1L,
            companyId,
            "phone",
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyList(),
            2L,
            1L,
            true,
            false,
            clientId,
            agencyId,
            Instant.now(),
            Collections.emptyMap(),
            null,
            null
          )
        ),
        Some(
          RealtySiteEvent
            .newBuilder()
            .setEventId(eventId)
            .setTimestamp(timestamp.build())
            .setSiteId(siteId.toLong)
            .setStatType(RealtySiteEvent.StatType.OFFER_SHOW)
            .setAgencyId(Int64Value.of(agencyId))
            .setClientId(Int64Value.of(clientId))
            .build()
        )
      )
    )

  "SiteEventConverterSpec" should {
    forAll(eventTestData) {
      (desc: String, event: Event, campaignOpt: Option[Campaign], expected: Option[RealtySiteEvent]) =>
        s"convert event $desc" in {
          val siteEventOpt = SiteEventConverter.convert(event, campaignOpt)
          siteEventOpt shouldBe expected
        }
    }

    "get exception for event with invalid site id" in {
      val builder = Event
        .newBuilder()
        .setEventId(eventId)
        .setTimestamp(timestamp)
        .setEventType(EventTypeNamespace.EventType.CARD_SHOW)
      builder.getObjectInfoBuilder.getSiteInfoBuilder.setSiteId("invalidSiteId")
      val event = builder.build()

      val tryConvert = Try(SiteEventConverter.convert(event, None))
      tryConvert.isFailure shouldBe true
      tryConvert.failed.get.getClass shouldBe classOf[NumberFormatException]
      tryConvert.failed.get.getMessage shouldBe "For input string: \"invalidSiteId\""
    }
  }
}
