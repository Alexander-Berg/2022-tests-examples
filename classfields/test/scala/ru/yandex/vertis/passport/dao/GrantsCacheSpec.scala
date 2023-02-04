package ru.yandex.vertis.passport.dao

import org.joda.time.DateTime
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.SpecBase

trait GrantsCacheSpec extends WordSpec with SpecBase {

  val grantsCache: GrantsCache

  "GrantsCache" should {
    "store grants" in {
      val time = DateTime.now().plusMinutes(5)
      grantsCache.upsert(GrantsCache.GrantsData(Map(), time)).futureValue

      val res = grantsCache.get().futureValue
      res.get.updated shouldBe time
    }
  }
}
