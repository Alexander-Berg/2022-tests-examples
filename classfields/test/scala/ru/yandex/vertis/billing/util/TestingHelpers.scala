package ru.yandex.vertis.billing.util

import ru.yandex.vertis.billing.dao.BilledEventDao.BilledEventInfoWithPayload
import ru.yandex.vertis.billing.event.EventRecord
import ru.yandex.vertis.billing.model_core.{
  CampaignEvents,
  Correction,
  Epoch,
  Incoming,
  OrderTransaction,
  Overdraft,
  Payload,
  Rebate,
  Withdraw,
  Withdraw2
}

object TestingHelpers {

  implicit class RichOrderTransaction(tr: OrderTransaction) {

    def withoutEpoch: OrderTransaction = {
      tr match {
        case i: Incoming =>
          i.copy(epoch = None)
        case c: Correction =>
          c.copy(epoch = None)
        case r: Rebate =>
          r.copy(epoch = None)
        case w: Withdraw =>
          w.copy(epoch = None)
        case w: Withdraw2 =>
          w.copy(epoch = None)
        case o: Overdraft =>
          o.copy(epoch = None)
      }
    }

    def extractEpoch: Option[Epoch] = {
      tr match {
        case i: Incoming =>
          i.epoch
        case c: Correction =>
          c.epoch
        case r: Rebate =>
          r.epoch
        case w: Withdraw =>
          w.epoch
        case w: Withdraw2 =>
          w.epoch
        case o: Overdraft =>
          o.epoch
      }
    }

  }

  implicit class RichOrderCampaignEvents(events: CampaignEvents) {

    def withoutEpoch: CampaignEvents = {
      CampaignEvents(events.snapshot, events.stat.copy(epoch = None))
    }

  }

  implicit class RichBilledEventInfoWithPayload(eventInfoWithPayload: BilledEventInfoWithPayload) {

    def withoutEpoches: BilledEventInfoWithPayload = {
      eventInfoWithPayload.withoutEventEpoch.withoutPayloadEpoch
    }

    def withoutEventEpoch: BilledEventInfoWithPayload = {
      eventInfoWithPayload.copy(
        billedEventInfo = eventInfoWithPayload.billedEventInfo.copy(epoch = None)
      )
    }

    def withoutPayloadEpoch: BilledEventInfoWithPayload = {
      val changedPayload = eventInfoWithPayload.payload.map(_.copy(epoch = None))
      eventInfoWithPayload.copy(payload = changedPayload)
    }

  }

  implicit class RichEventRecord(r: EventRecord) {

    def withoutEpoch: EventRecord = {
      r.copy(epoch = None)
    }

  }

  implicit class RichPayload(r: Payload) {

    def withoutEpoch: Payload = {
      r.copy(epoch = None)
    }

  }

}
