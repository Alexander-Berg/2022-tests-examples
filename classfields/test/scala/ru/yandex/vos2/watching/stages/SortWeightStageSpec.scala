package ru.yandex.vos2.watching.stages

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.watching.ProcessingState

import scala.concurrent.duration._

/**
  * Created by Vsevolod Levin on 26.03.2018.
  */
@RunWith(classOf[JUnitRunner])
class SortWeightStageSpec extends WordSpec with Matchers {

  "SortWeightStage" should {

    "calculate delay for offer expiration" in {
      val now = System.currentTimeMillis()
      val offerTtl = 15.days
      val offer = createExpiringOffer(now, offerTtl)

      val delay = getExpireSoonDelay(now, offerTtl)

      val result = SortWeightStage.process(ProcessingState(offer.build(), offer.build()))
      assert(result.delay <= delay && result.delay >= delay - 5.minutes)
    }

    "calculate delay for offer expiration if it is already near" in {
      val now = System.currentTimeMillis()
      val ttlDays = 3.days
      val offer = createExpiringOffer(now, ttlDays)

      val delay = ttlDays + 1.minute

      val result = SortWeightStage.process(ProcessingState(offer.build(), offer.build()))
      assert(result.delay <= delay && result.delay >= delay - 5.minutes)
    }

    "not calculate delay for expired offer" in {
      val now = System.currentTimeMillis() - 10000
      val ttl = 1.millis
      val offer = createExpiringOffer(now, ttl)

      val result = SortWeightStage.process(ProcessingState(offer.build(), offer.build()))
      assert(!result.delay.isFinite())
    }
  }

  private def createExpiringOffer(start: Long, ttl: FiniteDuration) = {
    val ttlHours = ttl.toHours.toInt
    val offer = TestUtils
      .createOffer()
      .setTimestampTtlStart(start)
      .setOfferTTLHours(ttlHours)
    offer
  }

  private def getExpireSoonDelay(start: Long, ttl: FiniteDuration) = {
    val accurateExpireTimestamp = (ttl - 5.days).toMillis + start
    (new DateTime(accurateExpireTimestamp).withTimeAtStartOfDay().getMillis - start).millis + 1.minute
  }
}
