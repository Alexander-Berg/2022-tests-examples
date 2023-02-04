package ru.yandex.vertis.ydb.skypper

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

@RunWith(classOf[JUnitRunner])
class PreloadSessionsTest extends AnyFunSuite with Matchers with InitTestYdb {

  test("preload sessions") {
    ydb.preloadSessions(25)
    val stats = ydb.stats
    stats.getAcquiredCount shouldBe 0
    stats.getIdleCount shouldBe 25
  }
}
