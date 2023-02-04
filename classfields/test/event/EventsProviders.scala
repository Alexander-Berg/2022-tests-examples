package ru.yandex.vertis.billing.event

import org.joda.time.DateTime
import ru.yandex.vertis.billing.event.Generator._
import ru.yandex.vertis.billing.event.failures.{BagTryHandler, LoggedBagTryHandler}
import ru.yandex.vertis.billing.microcore_model.Properties
import ru.yandex.vertis.billing.model_core.BaggagePayload.PhoneShowIdentifier
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.event.EventContext
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.IsoDateTimeFormatter
import ru.yandex.vertis.protobuf.kv.Converter

import scala.util.Try

/**
  * Generate events for tests
  *
  * @author alesavin
  */
trait EventsProviders {

  /**
    * Provide number of generated [[EventRecord]]s
    */
  def randomEventsReader(nrOfEvents: Int) = new Reader[EventRecord] {

    def read(interval: DateTimeInterval)(handle: EventRecord => Unit): Try[Unit] = Try {
      EventRecordGen
        .nextIterator(nrOfEvents)
        .foreach(handle)
    }
  }

  /**
    * Provide number generated [[EventRecord]]s sorted by its string representation
    */
  def sortedRandomEventsReader(nrOfEvents: Int) = new Reader[EventRecord] {

    def read(interval: DateTimeInterval)(handle: EventRecord => Unit): Try[Unit] = Try {
      val sorted = EventRecordGen
        .next(nrOfEvents)
        .toList
        .sortBy(_.toString)

      sorted.foreach(handle)
    }
  }

  /**
    * Provides fixed events reader
    */
  def fixedEventsReader[A](events: Iterable[A]) = new Reader[A] {

    def read(interval: DateTimeInterval)(handle: A => Unit): Try[Unit] = Try {
      events.foreach(handle)
    }
  }

  /** Change [[EventRecord]] offer id */
  def withOfferId(offerId: OfferId)(event: EventRecord): EventRecord =
    offerId match {
      case p: PartnerOfferId =>
        event.copy(values = {
          event.values
            .updated(Extractor.OfferIdCellName, p.offerId)
            .updated(Properties.BILLING_OFFER_ID, p.offerId)
            .updated(Extractor.OfferPartnerIdCellName, p.partnerId)
        })
      case b: Business =>
        event.copy(values = {
          event.values
            .updated(Extractor.OfferIdCellName, b.id)
            .updated(Properties.BILLING_OFFER_ID, b.id)
            .updated(Extractor.BusinessIdCellName, b.id)
        })
      case _ => event
    }

  /** Change [[EventRecord]] offer and position */
  def withOfferPosition(offerId: OfferId, position: Long)(event: EventRecord): EventRecord = {
    val e = withOfferId(offerId)(event)
    e.copy(values = e.values.updated(Extractor.OfferPositionCellName, position.toString))
  }

  def withDateTime(time: DateTime)(event: EventRecord): EventRecord =
    event.withValue(Extractor.TimestampCellName, IsoDateTimeFormatter.print(time))

  /** Change [[EventRecord]] click revenue */
  def withClickRevenue(revenue: Funds)(event: EventRecord): EventRecord =
    event.withValue(Properties.BILLING_CLICK_REVENUE, revenue.toString)

  /** Change [[EventRecord]] call revenue */
  def withCallRevenue(revenue: Funds)(event: EventRecord): EventRecord =
    event.withValue(Properties.BILLING_CALL_REVENUE, revenue.toString)

  /** Change [[EventRecord]] product */
  def withProduct(product: Product)(event: EventRecord): EventRecord = {
    val header = Extractor.getCampaignHeader(event).get
    val headerKv =
      Converter
        .toKeyValue(Conversions.toMessage(header.copy(product = product)), Some(Properties.BILLING_CAMPAIGN_HEADER))
        .get
    event.copy(values =
      event.values.filter(!_._1.startsWith(Properties.BILLING_CAMPAIGN_HEADER))
        ++ headerKv
    )
  }

  def withEventContext(context: EventContext)(event: EventRecord): EventRecord = {
    val contextKv = Converter.toKeyValue(Conversions.toMessage(context), Some(Properties.BILLING_EVENT_CONTEXT)).get
    event.copy(values = event.values ++ contextKv)
  }

  def withWorkPolicy(policy: Timetable)(event: EventRecord): EventRecord = {
    val context =
      Extractor
        .getEventContext(event)
        .copy(workPolicy = Some(policy))
    withEventContext(context)(event)
  }

  /** Change [[EventRecord]] campaign */
  def withCampaignOrder(campaignId: CampaignId, orderId: OrderId)(event: EventRecord): EventRecord = {
    val header = Extractor.getCampaignHeader(event).get
    val headerKv =
      Converter
        .toKeyValue(
          Conversions.toMessage(header.copy(id = campaignId, order = header.order.copy(id = orderId))),
          Some(Properties.BILLING_CAMPAIGN_HEADER)
        )
        .get
    event.copy(values =
      event.values.filter(!_._1.startsWith(Properties.BILLING_CAMPAIGN_HEADER))
        ++ headerKv
    )
  }

  def withDeadline(deadline: DateTime)(eventRecord: EventRecord) =
    withFields(Iterable((Properties.BILLING_ACTIVE_DEADLINE, IsoDateTimeFormatter.print(deadline))))(eventRecord)

  /** Change [[EventRecord]] specified fields */
  def withFields(fields: => Iterable[(String, String)])(event: EventRecord): EventRecord = {
    event.copy(values = fields.foldLeft(event.values) { (v, f) =>
      v.updated(f._1, f._2)
    })
  }

  /** Change [[EventRecord]] call settings */
  def withCallSettings(settings: => CallSettings)(event: EventRecord): EventRecord = {
    val header = Extractor.getCampaignHeader(event).get
    val headerKv =
      Converter
        .toKeyValue(
          Conversions.toMessage(header.copy(settings = header.settings.copy(callSettings = Some(settings)))),
          Some(Properties.BILLING_CAMPAIGN_HEADER)
        )
        .get
    event.copy(values =
      event.values.filter(!_._1.startsWith(Properties.BILLING_CAMPAIGN_HEADER))
        ++ headerKv
    )
  }

  def withPhoneShowId(id: PhoneShowIdentifier)(event: EventRecord) = {
    val identifier = Seq(Extractor.PhoneShowObjectId -> id.objectId) ++
      Seq(
        id.tag.map(t => Extractor.PhoneShowTag -> t),
        id.redirectPhone.map(r => Extractor.PhoneShowRedirectPhone -> r.value)
      ).flatten

    event.copy(values = event.values ++ identifier.toMap)
  }

  implicit def tryHandler =
    new BagTryHandler with LoggedBagTryHandler {
      def name = "Test"
    }

}
