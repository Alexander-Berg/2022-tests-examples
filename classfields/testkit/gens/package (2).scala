package auto.dealers.balance_alerts.model.testkit

import auto.dealers.balance_alerts.model.{BalanceAlert, BalanceEvent, BalanceEventType, DealerId}
import common.zio.clock.MoscowClock
import zio.random.Random
import zio.test.{Gen, Sized}

import java.time.{Instant, OffsetDateTime, ZonedDateTime}
import java.util.UUID

package object gens {
  type ZioGen[A] = Gen[zio.random.Random with zio.test.Sized, A]

  val eventTypeGen = Gen.fromIterable(BalanceEventType.values)
  val timestampGen = Gen.anyInstant.map(toOffsetDateTime)

  def eventGen(
      uuidG: ZioGen[UUID] = Gen.anyUUID,
      dealerIdG: ZioGen[DealerId] = Gen.anyLong,
      eventTypeG: ZioGen[BalanceEventType] = eventTypeGen,
      timestampG: ZioGen[OffsetDateTime] = timestampGen): ZioGen[BalanceEvent] =
    for {
      uuid <- uuidG
      dealerId <- dealerIdG
      eventType <- eventTypeG
      timestamp <- timestampG
    } yield BalanceEvent(uuid, dealerId, eventType, timestamp)

  def dealerEventsGen(dealerId: DealerId, events: Int = 3) =
    Gen.chunkOfN(events)(eventGen(dealerIdG = Gen.const(dealerId)))

  def dealerEventsMapGen(dealers: Int = 3) = {
    Gen
      .chunkOfN(dealers)(
        for {
          id <- Gen.anyLong
          events <- dealerEventsGen(id)
        } yield (id, events)
      )
      .map(_.toMap)
  }

  def alertsGen(
      uuidG: ZioGen[UUID] = Gen.anyUUID,
      dealerIdG: ZioGen[DealerId] = Gen.anyLong,
      eventTypeG: ZioGen[BalanceEventType] = eventTypeGen,
      timestampG: ZioGen[OffsetDateTime] = timestampGen,
      notifiedG: ZioGen[Option[OffsetDateTime]] = Gen.option(timestampGen),
      notificationsG: ZioGen[Int] = Gen.int(0, 3)): ZioGen[BalanceAlert] =
    for {
      uuid <- uuidG
      dealerId <- dealerIdG
      eventType <- eventTypeG
      timestamp <- timestampG
      lastNotified <- notifiedG
      notifications <- notificationsG
      count = if (lastNotified.isEmpty) 0 else notifications
    } yield BalanceAlert(uuid, dealerId, eventType, timestamp, lastNotified, count)

  def toOffsetDateTime(instant: Instant) =
    OffsetDateTime.ofInstant(instant, MoscowClock.timeZone)
}
