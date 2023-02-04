package ru.yandex.vertis.moisha.impl.autoru_users_subscriptions

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.impl.autoru_users.TestPolicies
import ru.yandex.vertis.moisha.impl.autoru_users_subscriptions.AutoRuUsersSubscriptionsPolicy.AutoRuUsersSubscriptionsRequest
import ru.yandex.vertis.moisha.impl.autoru_users_subscriptions.model._
import ru.yandex.vertis.moisha.model.DateTimeInterval
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.test.BaseSpec

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class RootAutoRuUsersSubscriptionsPolicyMatrixSpec extends BaseSpec with TestPolicies {

  private val baseCarsNewOffer =
    AutoRuUsersSubscriptionsOffer(
      category = Categories.Cars,
      section = Sections.New,
      mark = None,
      model = None,
      generation = None,
      year = None,
      geoId = None,
      price = None
    )

  "RootAutoRuUsersSubscriptionsPolicy" should {

    "return price of offers-history-reports-1 & contentType = VIN_HISTORY from matrix, without offer passed in request" in {
      calculatePrice(
        offer = None,
        AutoRuUsersOffersHistoryReportsContext(
          reportsCount = 1,
          geoId = 1,
          contentQuality = 1,
          ContextTypes.VinHistory,
          experiment = None
        )
      ).total shouldBe 13900
    }

    "return price of offers-history-reports-10 & contentType = VIN_HISTORY from matrix, without offer passed in request" in {
      calculatePrice(
        offer = None,
        AutoRuUsersOffersHistoryReportsContext(
          reportsCount = 10,
          geoId = 1,
          contentQuality = 1,
          ContextTypes.VinHistory,
          experiment = None
        )
      ).total shouldBe 99000
    }

    "throw on contentType = OFFER_HISTORY, if no offer passed in request" in {
      tryCalculatePrice(
        offer = None,
        AutoRuUsersOffersHistoryReportsContext(
          reportsCount = 1,
          geoId = 1,
          contentQuality = 1,
          ContextTypes.OfferHistory,
          experiment = None
        )
      ).failure.exception shouldBe an[IllegalArgumentException]
    }

    "return different prices of offers-history-reports-1 & contentType = OFFER_HISTORY from matrix depending on offer price" in {
      val geoId = 11029

      val context =
        AutoRuUsersOffersHistoryReportsContext(
          reportsCount = 1,
          geoId = geoId,
          contentQuality = 3,
          ContextTypes.OfferHistory,
          experiment = None
        )

      val offer1000000 = baseCarsNewOffer.copy(geoId = Some(Seq(geoId)), price = Some(1000000.rubles))
      val offer2000000 = offer1000000.copy(price = Some(2000000.rubles))

      calculatePrice(Some(offer1000000), context).total shouldBe 14700
      calculatePrice(Some(offer2000000), context).total shouldBe 19700
    }

    "throw on incorrect reportsCount" in {
      tryCalculatePrice(
        offer = None,
        AutoRuUsersOffersHistoryReportsContext(
          reportsCount = 777,
          geoId = 1,
          contentQuality = 1,
          ContextTypes.VinHistory,
          experiment = None
        )
      ).failure.exception shouldBe an[IllegalArgumentException]
    }

    "fallback to default product prices for supported given amounts" in {
      val givenOffer = Some(baseCarsNewOffer)
      def givenContext(reportsCount: Amount) = AutoRuUsersOffersHistoryReportsContext(
        reportsCount,
        geoId = 1,
        contentQuality = 100500,
        ContextTypes.OfferHistory,
        experiment = None
      )

      val testCases = Table(
        ("reportsCount", "expectedPrice", "expectedDuration"),
        (1, 13900, 365),
        (5, 59900, 365),
        (10, 99000, 365),
        (50, 299000, 60)
      )

      forAll(testCases) { (reportsCount, expectedPrice, expectedDuration) =>
        calculatePrice(givenOffer, givenContext(reportsCount)) should have(
          'total (expectedPrice),
          'duration (expectedDuration)
        )
      }
    }

    "should select price without experiment" in {
      tryCalculatePrice(
        None,
        context = AutoRuUsersOffersHistoryReportsContext(
          reportsCount = 5,
          geoId = 1,
          contentQuality = 1,
          ContextTypes.VinHistory,
          experiment = None
        )
      ).success.value.points.head.product.total shouldBe (299000)
    }

    "should select without experiment if request contains experiment but in matrix be away" in {
      val res = tryCalculatePrice(
        None,
        context = AutoRuUsersOffersHistoryReportsContext(
          reportsCount = 5,
          geoId = 1,
          contentQuality = 1,
          ContextTypes.VinHistory,
          experiment = Some("EXP777")
        )
      ).success.value
      res.points.head.product.total shouldBe (299000)
      res.points.head.experimentId shouldBe None
    }

    "should select price with experiment = EXP1" in {
      val res = tryCalculatePrice(
        None,
        context = AutoRuUsersOffersHistoryReportsContext(
          reportsCount = 5,
          geoId = 1,
          contentQuality = 1,
          ContextTypes.VinHistory,
          experiment = Some("EXP1")
        )
      ).success.value
      res.points.head.product.total shouldBe (399000)
      res.points.head.experimentId shouldBe Some("EXP1")
    }
    "should select price with experiment = EXP2" in {
      val res = tryCalculatePrice(
        None,
        context = AutoRuUsersOffersHistoryReportsContext(
          reportsCount = 5,
          geoId = 1,
          contentQuality = 1,
          ContextTypes.VinHistory,
          experiment = Some("EXP2")
        )
      ).success.value
      res.points.head.product.total shouldBe (499000)
      res.points.head.experimentId shouldBe Some("EXP2")
    }

    "fail with IllegalArgumentException when fallback product prices map does not contain product price for given amount" in {
      val givenAmount = 3

      val givenOffer = Some(baseCarsNewOffer)
      val givenContext = AutoRuUsersOffersHistoryReportsContext(
        reportsCount = givenAmount,
        geoId = 1,
        contentQuality = 100500,
        ContextTypes.OfferHistory,
        experiment = None
      )

      val exception = tryCalculatePrice(givenOffer, givenContext).failure.exception

      exception shouldBe an[IllegalArgumentException]
      exception.getMessage shouldBe s"Invalid amount 3 for offers-history-reports"
    }
  }

  private def calculatePrice(
      offer: Option[AutoRuUsersSubscriptionsOffer],
      context: AutoRuUsersOffersHistoryReportsContext) = {
    tryCalculatePrice(offer, context).success.value.points.loneElement.product
  }

  private def tryCalculatePrice(
      offer: Option[AutoRuUsersSubscriptionsOffer],
      context: AutoRuUsersOffersHistoryReportsContext) =
    Try {
      val request = AutoRuUsersSubscriptionsRequest(
        Products.OffersHistoryReports,
        MaybeAutoRuUsersSubscriptionsOffer(offer),
        context,
        DateTimeInterval.dayIntervalFrom(DateTime.now().withTimeAtStartOfDay())
      )
      usersSubscriptionsPolicy.estimate(request)
    }.flatten
}
