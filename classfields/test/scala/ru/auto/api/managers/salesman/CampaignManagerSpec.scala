package ru.auto.api.managers.salesman

import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.billing.LightResponseModel
import ru.auto.api.exceptions.{ApplyProductNotAllowedException, OrderNotFound, StatusConflict}
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.AutoruProduct.{TradeInRequestCarsNew, TradeInRequestCarsUsed}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.billing.BalanceId
import ru.auto.api.model.billing.vsbilling.OrderId
import ru.auto.api.model.gen.DeprecatedBillingModelGenerators.{campaignHeaderGen, orderGen}
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.model.salesman.convertCampaigns
import ru.auto.api.model.{AutoruDealer, AutoruProduct}
import ru.auto.api.services.billing.VsBillingClient.CampaignPatch
import ru.auto.api.services.billing.VsBillingInternalClient.ProductTypeFilter.CustomType
import ru.auto.api.services.billing.{MoishaClient, VsBillingClient, VsBillingInternalClient}
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.util.ManagerUtils
import ru.yandex.vertis.billing.Model.InactiveReason
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class CampaignManagerSpec extends BaseSpec with MockitoSupport with TestRequest with ScalaCheckPropertyChecks {

  private val salesmanClient = mock[SalesmanClient]
  private val cabinetClient = mock[CabinetApiClient]
  private val vsBillingClient = mock[VsBillingClient]
  private val vsBillingInternalClient = mock[VsBillingInternalClient]
  private val moishaClient = mock[MoishaClient]

  private val campaignManager =
    new CampaignManager(salesmanClient, cabinetClient, vsBillingClient, vsBillingInternalClient, moishaClient)

  after {
    reset(salesmanClient)
    reset(cabinetClient)
    reset(vsBillingClient)
    reset(vsBillingInternalClient)
    reset(moishaClient)
  }

  "CampaignManager.getCampaigns()" should {

    "get campaigns" in {
      forAll(DealerUserRefGen, Gen.listOf(CampaignGen).map(_.toSet)) { (dealer, campaigns) =>
        when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(campaigns)
        val apiCampaigns = campaignManager.getCampaigns(dealer).futureValue.getCampaignsList
        apiCampaigns should contain theSameElementsAs convertCampaigns(campaigns).getCampaignsList.asScala
        verify(salesmanClient).getCampaigns(eq(dealer), eq(false))(?)
        verifyNoMoreInteractions(salesmanClient)
        reset(salesmanClient)
      }
    }
  }

  "CampaignManager.getProductCampaign()" should {
    "get product campaign" in {
      forAll(
        DealerUserRefGen,
        clientGen,
        balanceClientGen,
        callsCarsNewCampaignGen,
        campaignHeaderGen,
        moishaPointGen(10000L)
      ) { (dealer, client, genBalanceClient, genCallsCarsNew, genProductCampaign, moishaPoint) =>
        val balanceClient = genBalanceClient.copy(accountId = Some(111L))
        val carsNewCalls = genCallsCarsNew.copy(enabled = true)

        val productCampaign = genProductCampaign.toBuilder
          .setSettings {
            genProductCampaign.toBuilder.getSettingsBuilder
              .setIsEnabled(true)
              .build()
          }
          .build()

        when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(carsNewCalls))
        when(cabinetClient.getBalanceClient(?)(?)).thenReturnF(balanceClient)
        when(cabinetClient.getClient(?)(?)).thenReturnF(client)
        when(vsBillingInternalClient.getCampaigns(?, ?, ?)(?)).thenReturnF(Iterable(productCampaign))
        when(moishaClient.getPriceWithoutOffer(?, ?, ?)(?, ?)).thenReturnF(moishaPoint)

        val expectedResponse =
          LightResponseModel.CampaignResponse
            .newBuilder()
            .setProduct("trade-in-request:cars:new")
            .setCost(100L)
            .setIsEditable(false)
            .setCampaign(
              LightResponseModel.CampaignHeader
                .newBuilder()
                .setIsEnabled(true)
                .setIsActive(true)
                .build()
            )
            .build()

        campaignManager
          .getProductCampaign(dealer, TradeInRequestCarsNew, withSuccessResponseStatus = false)
          .futureValue shouldBe expectedResponse
      }
    }

    "get product campaign without vs-billing campaign" in {
      forAll(
        DealerUserRefGen,
        clientGen,
        balanceClientGen,
        callsCarsNewCampaignGen,
        moishaPointGen(10000L)
      ) { (dealer, client, genBalanceClient, genCallsCarsNew, moishaPoint) =>
        val balanceClient = genBalanceClient.copy(accountId = Some(111L))
        val carsNewCalls = genCallsCarsNew.copy(enabled = true)

        when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(carsNewCalls))
        when(cabinetClient.getBalanceClient(?)(?)).thenReturnF(balanceClient)
        when(cabinetClient.getClient(?)(?)).thenReturnF(client)
        when(vsBillingInternalClient.getCampaigns(?, ?, ?)(?)).thenReturnF(Nil)
        when(moishaClient.getPriceWithoutOffer(?, ?, ?)(?, ?)).thenReturnF(moishaPoint)

        val expectedResponse =
          LightResponseModel.CampaignResponse
            .newBuilder()
            .setProduct("trade-in-request:cars:new")
            .setCost(100L)
            .setIsEditable(false)
            .build()

        campaignManager
          .getProductCampaign(dealer, TradeInRequestCarsNew, withSuccessResponseStatus = false)
          .futureValue shouldBe expectedResponse
      }
    }

    "fail on too many campaigns in vs-billing" in {
      forAll(
        DealerUserRefGen,
        clientGen,
        balanceClientGen,
        callsCarsNewCampaignGen,
        Gen.listOfN(3, campaignHeaderGen),
        moishaPointGen(10000L)
      ) { (dealer, client, genBalanceClient, genCallsCarsNew, campaigns, moishaPoint) =>
        val balanceClient = genBalanceClient.copy(accountId = Some(111L))
        val carsNewCalls = genCallsCarsNew.copy(enabled = true)

        when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(carsNewCalls))
        when(cabinetClient.getBalanceClient(?)(?)).thenReturnF(balanceClient)
        when(cabinetClient.getClient(?)(?)).thenReturnF(client)
        when(vsBillingInternalClient.getCampaigns(?, ?, ?)(?)).thenReturnF(Nil)
        when(vsBillingInternalClient.getCampaigns(?, ?, ?)(?)).thenReturnF(campaigns)
        when(moishaClient.getPriceWithoutOffer(?, ?, ?)(?, ?)).thenReturnF(moishaPoint)

        campaignManager
          .getProductCampaign(dealer, TradeInRequestCarsNew, withSuccessResponseStatus = false)
          .failed
          .futureValue shouldBe a[StatusConflict]
      }
    }
  }

  "CampaignManager.getProductCampaignsList()" should {
    "get product campaigns list" in {
      forAll(
        DealerUserRefGen,
        clientGen,
        balanceClientGen,
        callsCarsNewCampaignGen
      ) { (dealer, client, genBalanceClient, genCallsCarsNew) =>
        val balanceClient = genBalanceClient.copy(accountId = Some(111L))
        val carsNewCalls = genCallsCarsNew.copy(enabled = true)

        when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(carsNewCalls))
        when(cabinetClient.getBalanceClient(?)(?)).thenReturnF(balanceClient)
        when(cabinetClient.getClient(?)(?)).thenReturnF(client)

        val products: List[AutoruProduct] = List(
          TradeInRequestCarsNew,
          TradeInRequestCarsUsed
        )

        val prices = Map(
          TradeInRequestCarsNew -> moishaPointGen(10000L).next,
          TradeInRequestCarsUsed -> moishaPointGen(5000L).next
        )

        val editableCampaign = Map(
          TradeInRequestCarsNew -> false,
          TradeInRequestCarsUsed -> true
        )

        val expectedCampaigns = products.map { product =>
          val campaign = campaignHeaderGen.next

          val productCampaign = campaign.toBuilder
            .setSettings {
              campaign.toBuilder.getSettingsBuilder
                .setIsEnabled(true)
                .build()
            }
            .build()

          when(vsBillingInternalClient.getCampaigns(?, ?, eq(CustomType(product.name)))(?))
            .thenReturnF(Iterable(productCampaign))

          when(moishaClient.getPriceWithoutOffer(?, ?, eq(product))(?, ?))
            .thenReturnF(prices(product))

          LightResponseModel.CampaignResponse
            .newBuilder()
            .setProduct(product.name)
            .setCost(prices(product).product.total / 100)
            .setIsEditable(editableCampaign(product))
            .setCampaign(
              LightResponseModel.CampaignHeader
                .newBuilder()
                .setIsEnabled(true)
                .setIsActive(true)
                .build()
            )
            .build()
        }

        val expected = LightResponseModel.CampaignResponseListing
          .newBuilder()
          .setStatus(LightResponseModel.ResponseStatus.SUCCESS)
          .addAllItems(expectedCampaigns.asJava)
          .build()

        campaignManager.getProductCampaignsList(dealer, products).futureValue shouldBe expected
      }
    }
  }

  "CampaignManager.toCampaignResponse()" should {
    "get response for active campaign" in {
      forAll(ProductGen, campaignHeaderGen, BooleanGen, BooleanGen) {
        (product, campaignHeader, isCampaignEnabled, isEditable) =>
          val campaign = campaignHeader.toBuilder
            .clearInactiveReason()
            .setSettings {
              campaignHeader.toBuilder.getSettingsBuilder
                .setIsEnabled(isCampaignEnabled)
                .build()
            }
            .build()

          val expected = LightResponseModel.CampaignResponse
            .newBuilder()
            .setStatus(LightResponseModel.ResponseStatus.SUCCESS)
            .setProduct(product.name)
            .setCost(500L)
            .setIsEditable(isEditable)
            .setCampaign {
              LightResponseModel.CampaignHeader
                .newBuilder()
                .setIsEnabled(isCampaignEnabled)
                .setIsActive(true)
                .build()
            }
            .build()

          campaignManager.toCampaignResponse(
            product,
            Some(campaign),
            price = 50000L,
            isEditable
          ) shouldBe expected
      }
    }

    "get response for inactive campaign" in {
      forAll(ProductGen, campaignHeaderGen, BooleanGen, BooleanGen) {
        (product, campaignHeader, isCampaignEnabled, isEditable) =>
          val campaign = campaignHeader.toBuilder
            .setInactiveReason(InactiveReason.NO_ENOUGH_FUNDS)
            .setSettings {
              campaignHeader.toBuilder.getSettingsBuilder
                .setIsEnabled(isCampaignEnabled)
                .build()
            }
            .build()

          val expected = LightResponseModel.CampaignResponse
            .newBuilder()
            .setProduct(product.name)
            .setCost(500L)
            .setIsEditable(isEditable)
            .setCampaign {
              LightResponseModel.CampaignHeader
                .newBuilder()
                .setIsEnabled(isCampaignEnabled)
                .setIsActive(false)
                .setInactiveReason(InactiveReason.NO_ENOUGH_FUNDS)
                .build()
            }
            .build()

          campaignManager.toCampaignResponse(
            product,
            Some(campaign),
            price = 50000L,
            isEditable,
            withSuccessResponseStatus = false
          ) shouldBe expected
      }
    }

    "get response for empty campaign" in {
      forAll(ProductGen, BooleanGen) { (product, isEditable) =>
        val expected = LightResponseModel.CampaignResponse
          .newBuilder()
          .setStatus(LightResponseModel.ResponseStatus.SUCCESS)
          .setProduct(product.name)
          .setCost(500L)
          .setIsEditable(isEditable)
          .build()

        campaignManager.toCampaignResponse(
          product,
          productCampaign = None,
          price = 50000L,
          isEditable,
          withSuccessResponseStatus = true
        ) shouldBe expected
      }
    }
  }

  "CampaignManager.isCampaignEditable()" should {
    "return TRUE for all products except trade-in for cars new" in {
      val dealer = AutoruDealer(1L)
      val products = AutoruProduct.values.filterNot(_ == TradeInRequestCarsNew)

      products.foreach(product => campaignManager.isCampaignEditable(dealer, product).futureValue shouldBe true)
    }

    "return TRUE if dealer has CARS NEW tariff and SINGLE payment model" in {
      val dealer = AutoruDealer(1L)
      val campaign = singleCarsNewCampaignGen.next.copy(enabled = true)

      when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(campaign))

      campaignManager.isCampaignEditable(dealer, TradeInRequestCarsNew).futureValue shouldBe true
    }

    "return TRUE if dealer has CARS NEW tariff and QUOTA payment model" in {
      val dealer = AutoruDealer(1L)
      val campaign = quotaCarsNewCampaignGen.next.copy(enabled = true)

      when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(campaign))

      campaignManager.isCampaignEditable(dealer, TradeInRequestCarsNew).futureValue shouldBe true
    }

    "return FALSE if dealer has CARS NEW tariff and CALLS payment model" in {
      val dealer = AutoruDealer(1L)
      val campaign = callsCarsNewCampaignGen.next.copy(enabled = true)

      when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(campaign))

      campaignManager.isCampaignEditable(dealer, TradeInRequestCarsNew).futureValue shouldBe false
    }

    "return TRUE if dealer has disabled CARS NEW tariff" in {
      val dealer = AutoruDealer(1L)
      val campaign = Gen.oneOf(singleCarsNewCampaignGen, quotaCarsNewCampaignGen).next.copy(enabled = false)

      when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(campaign))

      campaignManager.isCampaignEditable(dealer, TradeInRequestCarsNew).futureValue shouldBe false
    }

    "return FALSE if dealer has no CARS NEW tariff" in {
      val dealer = AutoruDealer(1L)

      when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set())

      campaignManager.isCampaignEditable(dealer, TradeInRequestCarsNew).futureValue shouldBe false
    }
  }

  "CampaignManager.setProductCampaignStatus()" should {
    "return TRUE for editable campaign operation" in {
      forAll(DealerUserRefGen, ProductGen, campaignHeaderGen, orderGen, balanceClientGen, BooleanGen) {
        (dealer, product, campaignHeader, order, balanceClient, enabled) =>
          val editableCampaign = quotaCarsNewCampaignGen.next.copy(enabled = true)
          val client = balanceClient.copy(accountId = Some(1L))

          when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(editableCampaign))
          when(cabinetClient.getBalanceClient(?)(?)).thenReturnF(client)
          when(vsBillingInternalClient.getOrCreateOrder(?)(?)).thenReturnF(order)
          when(vsBillingInternalClient.getOrCreateCampaign(?)(?)).thenReturnF(campaignHeader)

          when {
            vsBillingClient.updateCampaign(
              BalanceId(eq(client.balanceClientId.value)),
              eq(client.balanceAgencyId),
              eq(campaignHeader.getId),
              eq(CampaignPatch(OrderId(1L), enabled = Some(enabled)))
            )(?)
          }.thenReturnF(campaignHeader)

          campaignManager
            .setProductCampaignStatus(dealer, product, enabled)
            .futureValue shouldBe ManagerUtils.SuccessResponse
      }
    }

    "return TRUE for editable campaign operation [already enabled]" in {
      forAll(DealerUserRefGen, ProductGen, campaignHeaderGen, orderGen, balanceClientGen, BooleanGen) {
        (dealer, product, campaignHeader, order, balanceClient, enabled) =>
          val editableCampaign = quotaCarsNewCampaignGen.next.copy(enabled = true)
          val client = balanceClient.copy(accountId = Some(1L))

          val campaign = campaignHeader.toBuilder
            .setSettings {
              campaignHeader.toBuilder.getSettingsBuilder
                .setIsEnabled(enabled)
                .build()
            }
            .build()

          when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(editableCampaign))
          when(cabinetClient.getBalanceClient(?)(?)).thenReturnF(client)
          when(vsBillingInternalClient.getOrCreateOrder(?)(?)).thenReturnF(order)
          when(vsBillingInternalClient.getOrCreateCampaign(?)(?)).thenReturnF(campaign)

          campaignManager
            .setProductCampaignStatus(dealer, product, enabled)
            .futureValue shouldBe ManagerUtils.SuccessResponse
      }
    }

    "fails on non-editable campaign [Trade-In]" in {
      forAll(DealerUserRefGen, campaignHeaderGen, orderGen, balanceClientGen, BooleanGen) {
        (dealer, campaignHeader, order, balanceClient, enabled) =>
          val nonEditableCampaign = quotaCarsNewCampaignGen.next.copy(enabled = false)

          when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(nonEditableCampaign))

          campaignManager
            .setProductCampaignStatus(dealer, TradeInRequestCarsNew, enabled)
            .failed
            .futureValue shouldBe a[ApplyProductNotAllowedException]
      }
    }

    "fails on fetch client account id" in {
      forAll(DealerUserRefGen, ProductGen, campaignHeaderGen, orderGen, balanceClientGen, BooleanGen) {
        (dealer, product, campaignHeader, order, balanceClient, enabled) =>
          val editableCampaign = quotaCarsNewCampaignGen.next.copy(enabled = true)
          val client = balanceClient.copy(accountId = None)

          when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(Set(editableCampaign))
          when(cabinetClient.getBalanceClient(?)(?)).thenReturnF(client)
          when(vsBillingInternalClient.getOrCreateOrder(?)(?)).thenReturnF(order)
          when(vsBillingInternalClient.getOrCreateCampaign(?)(?)).thenReturnF(campaignHeader)

          campaignManager
            .setProductCampaignStatus(dealer, product, enabled)
            .failed
            .futureValue shouldBe a[OrderNotFound]
      }
    }
  }
}
