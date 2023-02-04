package ru.auto.api.managers.vin

import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.VinHistoryApplyResponse.PaymentStatus
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse, VinHistoryApplyResponse, VinHistoryPaymentStatusResponse}
import ru.auto.api.exceptions.NotEnoughFundsOnAccount
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.billing.subscription.SubscriptionManager
import ru.auto.api.managers.carfax.PurchaseInfo
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.auto.salesman.model.user.ApiModel.{VinHistoryBoughtReport, VinHistoryBoughtReports}
import ru.yandex.vertis.mockito.MockitoSupport

class VinResolutionWalletManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport
  with TestRequest {

  private val subscriptionManager = mock[SubscriptionManager]
  private val salesmanClient = mock[SalesmanClient]

  private val vinResolutionWalletManager =
    new VinResolutionWalletManager(subscriptionManager, salesmanClient)

  "VinResolutionWalletManager.tryToBuyVinResolution()" should {
    "buy vin-history for dealer" in {
      forAll(OfferIDGen, VinGenerator) { (offerId, vin) =>
        when(salesmanClient.applyVinHistoryProduct(?, ?, ?)(?))
          .thenReturnF(
            VinHistoryApplyResponse
              .newBuilder()
              .setPaymentStatus(PaymentStatus.OK)
              .setStatus(ResponseStatus.SUCCESS)
              .build()
          )
        val result = vinResolutionWalletManager.buy(Some(offerId), vin, 0)(dealerRequest).futureValue
        result.isBought shouldBe true
      }
    }

    "buy offers-history-reports for user" in {
      forAll(OfferIDGen, VinGenerator) { (offerId, vin) =>
        when(subscriptionManager.saveBoughtVinHistoryReport(?, ?, ?, ?, ?, ?)(?))
          .thenReturnF(SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build())
        val result = vinResolutionWalletManager.buy(Some(offerId), vin, 1)(userRequest).futureValue
        result.isBought shouldBe true
      }
    }

    "not buy anything for anon user" in {
      forAll(OfferIDGen, VinGenerator) { (offerId, vin) =>
        val result = vinResolutionWalletManager.buy(Some(offerId), vin, 0)(anonymousRequest).futureValue
        result shouldBe PurchaseInfo.NotBought
      }
    }

    "not buy anything for empty vin" in {
      forAll(OfferIDGen) { (offerId) =>
        val result = vinResolutionWalletManager.buy(Some(offerId), "", 1)(userRequest).futureValue
        result shouldBe PurchaseInfo.NotBought
      }
    }

    "return false if not success" in {
      when(subscriptionManager.saveBoughtVinHistoryReport(?, ?, ?, ?, ?, ?)(?)).thenReturnF(
        SuccessResponse.newBuilder().setStatus(ResponseStatus.ERROR).build()
      )
      forAll(VinGenerator) { vin =>
        val res = vinResolutionWalletManager.buy(None, vin, 1)(userRequest).await
        res shouldBe PurchaseInfo.NotBought
      }
    }

    "return true if ok" in {
      when(subscriptionManager.saveBoughtVinHistoryReport(?, ?, ?, ?, ?, ?)(?)).thenReturnF(
        SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()
      )
      forAll(VinGenerator) { (vin) =>
        val res = vinResolutionWalletManager.buy(None, vin, 1)(userRequest).await
        res.isBought shouldBe true
      }
    }

    "return false if NotEnoughFundsOnAccount for dealer" in {
      forAll(VinGenerator) { vin =>
        when(salesmanClient.applyVinHistoryProduct(?, ?, ?)(?))
          .thenThrowF(new NotEnoughFundsOnAccount)
        val result = vinResolutionWalletManager.buy(None, vin, 0)(dealerRequest).futureValue
        result shouldBe PurchaseInfo.NotBought
      }
    }
  }

  "VinResolutionWalletManager.isVinResolutionAlreadyBought()" should {

    "check vin-history paid for dealer" in {
      forAll(OfferGen) { offer =>
        when(salesmanClient.isVinHistoryProductPaid(?, ?)(?))
          .thenReturnF(
            VinHistoryPaymentStatusResponse
              .newBuilder()
              .setPaymentStatus(
                VinHistoryPaymentStatusResponse.PaymentStatus.PAID
              )
              .setStatus(
                ResponseStatus.SUCCESS
              )
              .build()
          )
        val result = vinResolutionWalletManager
          .isVinResolutionAlreadyBought(offer)(dealerRequest)
          .futureValue
        result shouldBe PurchaseInfo(true, None)
      }
    }

    "check vin-resolution paid for user" in {
      reset(subscriptionManager)
      val offer = OfferGen.next.toBuilder
      offer.getDocumentsBuilder.setVin("2412AFSF")
      val offerEnriched = offer.build()
      when(subscriptionManager.getBoughtVinHistoryReports(?, ?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturnF(
          VinHistoryBoughtReports
            .newBuilder()
            .addReports(VinHistoryBoughtReport.newBuilder.setVin("2412AFSF"))
            .build
        )

      val result = vinResolutionWalletManager.isVinResolutionAlreadyBought(offerEnriched)(userRequest).futureValue
      result.isBought shouldBe true
      verify(subscriptionManager).getBoughtVinHistoryReports(?, ?, ?, ?, ?, ?, ?, ?)(?)
      verifyNoMoreInteractions(subscriptionManager)
    }

    "check vin-resolution paid for anonymous user" in {
      forAll(OfferGen) { offer =>
        val result = vinResolutionWalletManager
          .isVinResolutionAlreadyBought(offer)(anonymousRequest)
          .futureValue
        result shouldBe PurchaseInfo.NotBought
      }
    }
  }
}
