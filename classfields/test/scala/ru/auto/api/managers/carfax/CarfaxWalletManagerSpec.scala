package ru.auto.api.managers.carfax

import org.mockito.Mockito.reset
import org.scalatest.LoneElement
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.billing.subscription.SubscriptionManager
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.auto.salesman.model.user.ApiModel.{VinHistoryBoughtReport, VinHistoryBoughtReports}
import ru.yandex.vertis.mockito.MockitoSupport

class CarfaxWalletManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport
  with LoneElement
  with TestRequest {

  val subscriptionManager: SubscriptionManager = mock[SubscriptionManager]
  val salesmanClient: SalesmanClient = mock[SalesmanClient]

  val manager: CarfaxWalletManager =
    new CarfaxWalletManager(subscriptionManager, salesmanClient)

  val vin = "Z8T4C5S19BM005269"

  before {
    reset(subscriptionManager, salesmanClient)
  }

  val boughtEmpty: VinHistoryBoughtReports = VinHistoryBoughtReports.newBuilder().build()

  val bought: VinHistoryBoughtReports = VinHistoryBoughtReports
    .newBuilder()
    .addReports(
      VinHistoryBoughtReport
        .newBuilder()
        .setVin(vin)
        .build
    )
    .build()

  "CarfaxWalletManager.checkBought" should {
    "return false if anonym" in {
      val user = anonymousRequest
      val res = manager.getPurchaseInfo(vin)(user).await
      res shouldBe PurchaseInfo.NotBought
    }

    "return go to dealer path if dealer and return false" in {
      val user = dealerRequest
      when(salesmanClient.getBoughtVinHistoryReports(?, ?, ?, ?, ?, ?, ?)(?)).thenReturnF(boughtEmpty)
      val res = manager.getPurchaseInfo(vin)(user).await
      res shouldBe PurchaseInfo.NotBought
    }

    "return go to dealer path if dealer and return true" in {
      val user = dealerRequest
      when(salesmanClient.getBoughtVinHistoryReports(?, ?, ?, ?, ?, ?, ?)(?)).thenReturnF(bought)
      val res = manager.getPurchaseInfo(vin)(user).await
      res shouldBe PurchaseInfo(true, Some(0))
    }

    "return go to user path if user and return false" in {
      val user = userRequest
      when(subscriptionManager.getBoughtVinHistoryReports(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturnF(boughtEmpty)
      val res = manager.getPurchaseInfo(vin)(user).await
      res shouldBe PurchaseInfo.NotBought
    }

    "return go to user path if user and return true" in {
      val user = userRequest
      when(subscriptionManager.getBoughtVinHistoryReports(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturnF(bought)
      val res = manager.getPurchaseInfo(vin)(user).await
      res shouldBe PurchaseInfo(true, Some(0))
    }
  }

  "CarfaxWalletManager.getQuota" should {
    "return zero for anonym" in {
      val res = manager.getQuota(anonymousRequest).futureValue
      res shouldBe 0
    }

    "return zero for dealer" in {
      val res = manager.getQuota(dealerRequest).futureValue
      res shouldBe 0
    }

    "return quota by active vin-history package for registered user" in {
      when(subscriptionManager.getVinHistoryQuota(?)(?)).thenReturnF(66)
      val res = manager.getQuota(userRequest).futureValue
      res shouldBe 66
    }

    "return zero if user doesn't have active vin-history package" in {
      when(subscriptionManager.getVinHistoryQuota(?)(?)).thenReturnF(0)
      val res = manager.getQuota(userRequest).futureValue
      res shouldBe 0
    }
  }
}
