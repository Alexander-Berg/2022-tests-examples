package ru.yandex.vos2.autoru.utils.time

import org.joda.time.DateTime

class FixedTimeService(now: DateTime) extends TimeService {
  override def getNow: DateTime = now
}
