package vertis.zio.metering

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vertis.zio.metering.HistogramBuckets.{ExpBuckets, LinearBuckets}

import scala.concurrent.duration._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class HistogramBucketsSpec extends AnyWordSpec with Matchers {

  private def checkBuckets(buckets: HistogramBuckets, expectedList: Seq[Double]): Assertion =
    buckets.list shouldBe expectedList

  "LinearBuckets" should {
    "be linear" in {
      checkBuckets(LinearBuckets(0.second, 1.minute, 10.seconds), Seq(10, 20, 30, 40, 50, 60))
    }
    "check bounds" in intercept[IllegalArgumentException] {
      LinearBuckets(1.minute, 1.second, 10.seconds)
    }
    // but what for?
    "support a one value instances" in {
      checkBuckets(LinearBuckets(1.minute, 1.minute, 10.seconds), Seq(60))
    }
    "fail on too many buckets" in intercept[IllegalArgumentException] {
      LinearBuckets(0.second, 1.hour, 1.second)
    }
  }

  "ExponentialBuckets" should {
    "be exponential" in {
      checkBuckets(ExpBuckets(1.second, 1.minute, upperBoundAsBucket = false), Seq(1, 2, 4, 8, 16, 32))
    }
    "include upper bound by default" in {
      checkBuckets(ExpBuckets(1.second, 1.minute), Seq(1, 2, 4, 8, 16, 32, 60))
    }
    "not allow starting from 0" in intercept[IllegalArgumentException] {
      ExpBuckets(0.second, 1.minute)
    }
  }
}
