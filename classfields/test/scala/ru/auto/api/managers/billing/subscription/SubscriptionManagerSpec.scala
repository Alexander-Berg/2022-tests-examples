package ru.auto.api.managers.billing.subscription

import org.mockito.Mockito.{reset, verify}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.exceptions.OffersHistoryNotAllowed
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.AutoruProduct.OffersHistoryReports
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.SalesmanModelGenerators.ProductPriceGen
import ru.auto.api.model.{AutoruUser, DealerUserRoles, OfferID, UserInfo, UserRef, VarityResolution}
import ru.auto.api.services.salesman.SalesmanUserClient
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanDomain.AutoruSalesmanDomain
import ru.auto.api.util.Resources
import ru.auto.salesman.model.user.ApiModel.ProductResponses
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest.VinHistory
import ru.yandex.vertis.mockito.MockitoSupport

class SubscriptionManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with TestRequest
  with BeforeAndAfter {

  private val salesmanUserClient = mock[SalesmanUserClient]

  private val manager = new SubscriptionManager(salesmanUserClient)

  private def userInfo(userRef: UserRef) =
    UserInfo(
      ip = "",
      deviceUid = None,
      optSessionID = None,
      session = None,
      varityResolution = VarityResolution.Human,
      userRef,
      dealerRef = None,
      dealerUserRole = DealerUserRoles.Unknown
    )

  private def getVinHistoryPrices(userRef: UserRef, offerId: Option[OfferID], vinOrLicensePlate: Option[String]) = {
    manager.getAllAvailableSubscriptionsPrices(
      AutoruSalesmanDomain,
      userInfo(userRef),
      OffersHistoryReports,
      vinOrLicensePlate,
      offerId
    )
  }

  before {
    reset(salesmanUserClient)
  }

  "SubscriptionManager.getAllAvailableSubscriptionsPrices" should {

    "return prices for vin packages when car info isn't passed" in {
      forAll(Gen.nonEmptyListOf(ProductPriceGen), PrivateUserRefGen) { (prices, userRef) =>
        when(salesmanUserClient.getAllAvailableSubscriptionsPrices(?, ?, ?, ?)(?)).thenReturnF(prices)

        val res = getVinHistoryPrices(userRef, offerId = None, vinOrLicensePlate = None).futureValue

        (res.getProductPricesList should contain).theSameElementsInOrderAs(prices)
      }
    }

    "request salesman for vin-history prices when car info isn't passed" in {
      forAll(Gen.nonEmptyListOf(ProductPriceGen), PrivateUserRefGen) { (prices, userRef) =>
        reset(salesmanUserClient)
        when(salesmanUserClient.getAllAvailableSubscriptionsPrices(?, ?, ?, ?)(?)).thenReturnF(prices)
        val expectedPriceRequest = PriceRequest.newBuilder().setVinHistory(VinHistory.getDefaultInstance).build()

        val res = getVinHistoryPrices(userRef, offerId = None, vinOrLicensePlate = None).futureValue

        verify(salesmanUserClient).getAllAvailableSubscriptionsPrices(?, ?, ?, eq(expectedPriceRequest))(?)
        (res.getProductPricesList should contain).theSameElementsInOrderAs(prices)
      }
    }

    "not send a user, when it is an anonymous user" in {
      forAll(Gen.nonEmptyListOf(ProductPriceGen), AnonymousUserRefGen) { (prices, userRef) =>
        when(salesmanUserClient.getAllAvailableSubscriptionsPrices(?, eq(None), ?, ?)(?)).thenReturnF(prices)

        val res = getVinHistoryPrices(userRef, offerId = None, vinOrLicensePlate = None).futureValue

        (res.getProductPricesList should contain).theSameElementsInOrderAs(prices)
      }
    }

    "return exception, for a dealer" in {
      forAll(DealerUserRefGen) { userRef =>
        getVinHistoryPrices(
          userRef,
          offerId = None,
          vinOrLicensePlate = None
        ).failed.futureValue shouldBe an[OffersHistoryNotAllowed]
      }
    }
  }

  "SubscriptionManager.getVinHistoryQuota" should {

    "get full quota, based on all bought packages" in {
      val productResponses = Resources.toProto[ProductResponses]("/salesman/all_active_offers_history_reports.json")
      val user = AutoruUser(56795118)
      when(salesmanUserClient.getAllActiveProducts(?, ?, ?)(?)).thenReturnF(productResponses)

      val res = manager.getVinHistoryQuota(user).futureValue

      res shouldBe 7 + 10 // см. поле counter в файле all_active_offers_history_reports.json
    }
  }
}
