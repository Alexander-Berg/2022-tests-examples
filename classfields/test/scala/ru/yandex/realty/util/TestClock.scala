package ru.yandex.realty.util

import java.time.{Clock, Instant, ZoneId}
import java.util.concurrent.atomic.AtomicReference

class TestClock private (instantRef: AtomicReference[Instant], zoneId: ZoneId) extends Clock {

  def this(zoneId: ZoneId) = {
    this(new AtomicReference(Instant.EPOCH), zoneId)
  }

  override def getZone: ZoneId = zoneId

  override def withZone(zone: ZoneId): Clock = new TestClock(this.instantRef, this.zoneId)

  override def instant(): Instant = instantRef.get()

  def setInstant(instant: Instant): Unit = instantRef.set(instant)

}
