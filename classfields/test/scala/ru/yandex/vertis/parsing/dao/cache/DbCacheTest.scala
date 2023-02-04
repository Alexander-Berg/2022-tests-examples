package ru.yandex.vertis.parsing.dao.cache

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DbCacheTest extends FunSuite {
  test("getNextSyncMoment") {
    val moment = DateTime.now()
    val nextMoment = DbCache.getNextSyncMoment(moment)
    assert(nextMoment.getMillis > moment.plusHours(6).getMillis)
    assert(nextMoment.getMillisOfSecond == 0)
    assert(nextMoment.getSecondOfMinute == 0)
    assert(nextMoment.getMinuteOfHour % 2 == 0)
  }
}
