package ru.yandex.auto.extdata.jobs.sitemap.utils

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.OptionValues._
import org.scalactic.TypeCheckedTripleEquals._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.ops._
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.extdata.jobs.sitemap.utils.SitemapUtils.offerUpdateDateEachNDays

import java.time.{Duration, Instant}

@RunWith(classOf[JUnitRunner])
class SitemapUtilsSpec extends WordSpec with Matchers with ScalaCheckPropertyChecks {
  import SitemapUtilsSpec._

  "offerUpdateDateEachNDays" when {
    "currentInstant <= startInstant and n is positive" should {
      implicit val validArgs: Arbitrary[OfferUpdateDateEachNDaysArgs] = Arbitrary {
        for {
          startInstant <- Gen.javaInstant.between(Instant.EPOCH, Instant.MAX)
          n <- Gen.posNum[Long]
          currentInstant <- Gen.javaInstant.between(startInstant, Instant.MAX)
        } yield OfferUpdateDateEachNDaysArgs(currentInstant, n, startInstant)
      }

      "return an instant day of which is divisible by the given frequency starting from startInstant" in forAll {
        (args: OfferUpdateDateEachNDaysArgs) =>
          val maybeDurationMod = for {
            result <- offerUpdateDateEachNDays(args.currentInstant)(args.n)(args.startInstant)
            duration = Duration.between(args.startInstant, result).toDays
            durationModN = duration % args.n
          } yield durationModN

          maybeDurationMod should ===(Some(0L))
      }

      "return an instant that not greater than the current instant" in forAll { (args: OfferUpdateDateEachNDaysArgs) =>
        offerUpdateDateEachNDays(args.currentInstant)(args.n)(args.startInstant)
          .map(_.isAfter(args.currentInstant)) should ===(
          Some(false)
        )
      }

      "return an instant that not less than the starting instant" in forAll { (args: OfferUpdateDateEachNDaysArgs) =>
        offerUpdateDateEachNDays(args.currentInstant)(args.n)(args.startInstant)
          .map(_.isBefore(args.startInstant)) should ===(
          Some(false)
        )
      }

      "return the instant closest to the n-th day after the start date" in forAll {
        (args: OfferUpdateDateEachNDaysArgs) =>
          val daysUntilCurrent = for {
            updateDate <- offerUpdateDateEachNDays(args.currentInstant)(args.n)(args.startInstant)
            durationDays = Duration.between(updateDate, args.currentInstant).toDays
          } yield durationDays

          daysUntilCurrent.value should be < (args.n)
      }
    }

    "currentInstant > startInstant" should {
      implicit val validArgs: Arbitrary[OfferUpdateDateEachNDaysArgs] = Arbitrary {
        for {
          currentInstant <- Gen.javaInstant.between(Instant.EPOCH, Instant.MAX)
          n <- Gen.posNum[Long]
          startInstant <- Gen.javaInstant.between(currentInstant, Instant.MAX)
        } yield OfferUpdateDateEachNDaysArgs(currentInstant, n, startInstant)
      }

      "return None" in forAll { (args: OfferUpdateDateEachNDaysArgs) =>
        offerUpdateDateEachNDays(args.currentInstant)(args.n)(args.startInstant) should ===(None)
      }
    }

    "n is not positive" should {
      implicit val validArgs: Arbitrary[OfferUpdateDateEachNDaysArgs] = Arbitrary {
        for {
          currentInstant <- Gen.javaInstant.between(Instant.EPOCH, Instant.MAX)
          n <- Gen.chooseNum(Long.MinValue, 0)
          startInstant <- Gen.javaInstant.between(currentInstant, Instant.MAX)
        } yield OfferUpdateDateEachNDaysArgs(currentInstant, n, startInstant)
      }

      "return None" in { (args: OfferUpdateDateEachNDaysArgs) =>
        offerUpdateDateEachNDays(args.currentInstant)(args.n)(args.startInstant) should ===(None)
      }
    }
  }
}

object SitemapUtilsSpec {
  case class OfferUpdateDateEachNDaysArgs(currentInstant: Instant, n: Long, startInstant: Instant)
}
