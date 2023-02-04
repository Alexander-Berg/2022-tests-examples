package ru.auto.api.managers.vin

import java.time.OffsetDateTime

import com.google.protobuf.Duration
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.{reset, verify}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.{Assertion, LoneElement}
import ru.auto.api.ApiOfferModel.PaidServicePrice
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.PaidReason
import ru.auto.api.auth.Application
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.billing.subscription.SubscriptionManager
import ru.auto.api.managers.vin.HistoryReportPriceManagerSpec.{Kopecks, RichInt}
import ru.auto.api.model.ModelGenerators.OfferGen
import ru.auto.api.model.{AutoruDealer, AutoruProduct, RequestParams, UserRef}
import ru.auto.api.services.billing.MoishaClient
import ru.auto.api.services.billing.MoishaClient.{MoishaInterval, MoishaPoint, MoishaProduct, ProductDuration}
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.salesman.SalesmanUserClient
import ru.auto.api.util.RequestImpl
import ru.auto.api.vin.VinApiModel.ReportParams
import ru.auto.salesman.model.user.ApiModel
import ru.auto.salesman.model.user.ApiModel.{Price, ProductPrice}
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest
import ru.yandex.passport.model.common.CommonModel.{DomainBan, UserModerationStatus}
import ru.yandex.vertis.mockito.MockitoSupport._
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.math.BigDecimal.long2bigDecimal

//noinspection RedundantDefaultArgument
class HistoryReportPriceManagerSpec extends BaseSpec with TestRequest with LoneElement with ScalaCheckPropertyChecks {

  private val moishaClient = mock[MoishaClient]
  private val salesmanUserClient = mock[SalesmanUserClient]
  private val geobaseClient = mock[GeobaseClient]
  private val subscriptionManager = new SubscriptionManager(salesmanUserClient)
  private val manager = new HistoryReportPriceManager(moishaClient, subscriptionManager, geobaseClient)

  private val resellerUserModerationStatus = {
    val userModerationStatusBuilder = UserModerationStatus.newBuilder()
    userModerationStatusBuilder
      .setReseller(true)
      .putBans("CARS", DomainBan.newBuilder().addReasons("USER_RESELLER").build())
    userModerationStatusBuilder.build()
  }

  private val reportParams: ReportParams = ReportParams.newBuilder.build()

  private def moishaPoint(price: Int = 30000, days: Int = 180): MoishaPoint = MoishaPoint(
    "",
    MoishaInterval(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1)),
    MoishaProduct(AutoruProduct.VinHistory.toString, Seq.empty, price, Some(ProductDuration.days(days)))
  )

  private def productPrice(price: Kopecks, duration: FiniteDuration) =
    ProductPrice
      .newBuilder()
      .setPrice(
        Price
          .newBuilder()
          .setBasePrice(price.raw)
          .setEffectivePrice(price.raw)
          .build
      )
      .setDuration(Duration.newBuilder().setSeconds(duration.toSeconds))
      .setDays(duration.toDays.toIntExact)
      .build()

  "HistoryReportPriceManager.getPrices for report" should {

    "go to dealer path if dealer" in {
      when(geobaseClient.regionIdByLocation(?, ?)(?)).thenReturnF(55L)
      when(moishaClient.getPriceWithoutOffer(?, ?, ?)(?, ?)).thenReturnF(moishaPoint(price = 30000))
      val res = manager.getPrices(reportParams)(dealerRequest).futureValue
      res.head.getPrice shouldBe 300
    }

    "return duration in days" in {
      when(geobaseClient.regionIdByLocation(?, ?)(?)).thenReturnF(55L)
      when(moishaClient.getPriceWithoutOffer(?, ?, ?)(?, ?)).thenReturnF(moishaPoint(days = 180))
      val res = manager.getPrices(reportParams)(dealerRequest).futureValue
      res.loneElement.getDays shouldBe 180
    }

    def mockGetAllAvailableSubscriptionsPrices(price: Kopecks, duration: FiniteDuration): Unit = {
      reset(salesmanUserClient)
      when(salesmanUserClient.getAllAvailableSubscriptionsPrices(?, ?, ?, ?)(?))
        .thenReturnF(List(productPrice(price, duration)))
    }

    def assertPriceRequest(runAssertion: PriceRequest => Assertion): Unit = {
      verify(salesmanUserClient).getAllAvailableSubscriptionsPrices(
        ?,
        ?,
        ?,
        argThat[PriceRequest] { priceRequest =>
          runAssertion(priceRequest)
          // если assertion упадёт, досюда не дойдём;
          // если не упадёт, надо возвращать true, сигнализируя, что с аргументом всё ок
          true
        }
      )(?)
    }

    "return price in rubles from salesman-user-api to user" in {
      mockGetAllAvailableSubscriptionsPrices(1000.kopecks, 365.days)
      val res = manager.getPrices(reportParams)(userRequest).futureValue
      res.loneElement.getPrice shouldBe 10
    }

    "request salesman-user-api with ReportParams in request" in {
      mockGetAllAvailableSubscriptionsPrices(1000.kopecks, 365.days)
      manager.getPrices(reportParams)(userRequest).futureValue
      assertPriceRequest(_.getVinHistory.getReportParams shouldBe reportParams)
    }

    "request salesman-user-api with UserModerationStatus in request for reseller" in {
      mockGetAllAvailableSubscriptionsPrices(1000.kopecks, 365.days)
      manager.getPrices(reportParams)(resellerRequest(resellerUserModerationStatus)).futureValue
      assertPriceRequest(_.getUserModerationStatus shouldBe resellerUserModerationStatus)
    }

    "return duration in days for users" in {
      mockGetAllAvailableSubscriptionsPrices(1000.kopecks, 365.days)
      val res = manager.getPrices(reportParams)(userRequest).futureValue
      res.loneElement.getDays shouldBe 365
    }
  }

  "HistoryReportPriceManager.getPrices for offer" should {
    val price = PaidServicePrice
      .newBuilder()
      .setService(AutoruProduct.OffersHistoryReports.name)
      .setPrice(300)
      .setOriginalPrice(200)
      .setCounter(1)
      .setDays(180)
      .setCurrency("RUR")
      .setPaidReason(PaidReason.FREE_LIMIT)
      .build()

    val subscriptionManager = mock[SubscriptionManager]
    val manager = new HistoryReportPriceManager(moishaClient, subscriptionManager, geobaseClient)

    "get price for user" in {
      forAll(OfferGen) { offer =>
        when(subscriptionManager.getAllAvailableSubscriptionsPrices(?, ?, ?, ?)(?))
          .thenReturnF(
            List(
              ProductPrice
                .newBuilder()
                .setPrice(
                  ApiModel.Price
                    .newBuilder()
                    .setBasePrice(20000)
                    .setEffectivePrice(30000)
                )
                .setCounter(1)
                .setDays(180)
                .build()
            )
          )
        val result = manager.getPrices(offer, 2)(userRequest).futureValue
        result.head shouldBe price
      }
    }

    "get price for anon user" in {
      forAll(OfferGen) { offer =>
        when(subscriptionManager.getAllAvailableSubscriptionsPrices(?, ?, ?, ?)(?))
          .thenReturnF(
            List(
              ProductPrice
                .newBuilder()
                .setPrice(
                  ApiModel.Price
                    .newBuilder()
                    .setBasePrice(20000)
                    .setEffectivePrice(30000)
                )
                .setCounter(1)
                .setDays(180)
                .build()
            )
          )
        val result = manager.getPrices(offer, 2)(anonymousRequest).futureValue
        result.head shouldBe price
      }
    }

    "get price for dealer without location" in {
      forAll(OfferGen) { offer =>
        val dealerRequestWithoutLocation = {
          val r = new RequestImpl
          r.setRequestParams(RequestParams.construct("1.1.1.1"))
          r.setApplication(Application.iosApp)
          r.setDealer(AutoruDealer(123))
          r.setUser(UserRef.parse("dealer:123"))
          r.setTrace(Traced.empty)
          r
        }
        when(geobaseClient.regionIdByIp(?)(?))
          .thenReturnF(1)
        when(moishaClient.getPrice(?, ?, ?, ?, ?)(?, ?))
          .thenReturnF(
            MoishaPoint(
              "",
              MoishaInterval(OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1)),
              MoishaProduct(AutoruProduct.VinHistory.toString, Seq.empty, 30000, Some(ProductDuration.days(180)))
            )
          )

        val result = manager.getPrices(offer, 2)(dealerRequestWithoutLocation).futureValue
        result.head shouldBe price.toBuilder.clearOriginalPrice().build()
      }
    }
  }
}

object HistoryReportPriceManagerSpec {

  final case class Kopecks(raw: Int)

  implicit final class RichInt(private val price: Int) extends AnyVal {
    def kopecks: Kopecks = Kopecks(price)
  }
}
