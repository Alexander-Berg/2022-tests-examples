package ru.auto.api.managers.price

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.auth.Application
import ru.auto.api.model.{AutoruProduct, RequestParams, UserRef}
import ru.auto.api.util.RequestImpl
import ru.auto.api.BaseSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.SalesmanModelGenerators
import ru.auto.api.services.salesman.SalesmanUserClient
import ru.auto.salesman.model.user.ApiModel
import org.mockito.Mockito._
import ru.auto.api.services.passport.PassportClient
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus

class SalesmanUserPriceManagerSpec
  extends BaseSpec
  with ScalaCheckPropertyChecks
  with MockitoSupport
  with SalesmanModelGenerators {
  val salesmanUserClient: SalesmanUserClient = mock[SalesmanUserClient]
  val passportClient: PassportClient = mock[PassportClient]

  val salesmanUserPriceManager: SalesmanUserPriceManager =
    new SalesmanUserPriceManager(salesmanUserClient, passportClient)

  private val trace = Traced.empty

  private def generateReq(userRef: UserRef): RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r.setUser(userRef)
    r.setTrace(trace)
    r
  }

  private val notReseller = UserModerationStatus.newBuilder().setReseller(false).build
  before {
    reset(salesmanUserClient)
  }

  "SalesmanUserPriceManager getAllAvailableSubscriptionsPrices" should {
    "filter vip for MOTO offer" in {
      forAll(MotoOfferGen, ProductPriceGen) {
        case (offer, productPrice) =>
          implicit val request = generateReq(UserRef.parse(offer.getUserRef))
          val usedProductPrice =
            productPrice.toBuilder.setProduct(AutoruProduct.PackageVip.name).build
          val productPrices = ApiModel.ProductPrices
            .newBuilder()
            .addProductPrices(usedProductPrice)
            .setOfferId(offer.getId)
            .build

          when(salesmanUserClient.getMultipleOffersPrices(?, ?, ?)(?))
            .thenReturnF(Iterable(productPrices))
          when(passportClient.getUserModeration(?)(?)).thenReturnF(notReseller)
          val pricesMap = salesmanUserPriceManager
            .getMultipleOffersPrices(
              Seq(offer),
              Seq(AutoruProduct.PackageVip),
              applyMoneyFeature = false,
              isNewDraft = false
            )
            .futureValue
          pricesMap.toList.head._2 shouldBe empty
      }
    }

    "filter vip for TRUCKS offer" in {
      forAll(TruckOfferGen, ProductPriceGen) {
        case (offer, productPrice) =>
          implicit val request = generateReq(UserRef.parse(offer.getUserRef))
          val usedProductPrice =
            productPrice.toBuilder.setProduct(AutoruProduct.PackageVip.name).build
          val productPrices = ApiModel.ProductPrices
            .newBuilder()
            .addProductPrices(usedProductPrice)
            .setOfferId(offer.getId)
            .build

          when(passportClient.getUserModeration(?)(?)).thenReturnF(notReseller)
          when(salesmanUserClient.getMultipleOffersPrices(?, ?, ?)(?))
            .thenReturnF(Iterable(productPrices))
          val pricesMap = salesmanUserPriceManager
            .getMultipleOffersPrices(
              Seq(offer),
              Seq(AutoruProduct.PackageVip),
              applyMoneyFeature = false,
              isNewDraft = false
            )
            .futureValue
          pricesMap.toList.head._2 shouldBe empty
      }
    }

    "not filter vip for CARS offer" in {
      forAll(CarsOfferGen, ProductPriceGen) {
        case (offer, productPrice) =>
          implicit val request = generateReq(UserRef.parse(offer.getUserRef))
          val usedProductPrice =
            productPrice.toBuilder.setProduct(AutoruProduct.PackageVip.name).build
          val productPrices = ApiModel.ProductPrices
            .newBuilder()
            .addProductPrices(usedProductPrice)
            .setOfferId(offer.getId)
            .build

          when(passportClient.getUserModeration(?)(?)).thenReturnF(notReseller)
          when(salesmanUserClient.getMultipleOffersPrices(?, ?, ?)(?))
            .thenReturnF(Iterable(productPrices))
          val pricesMap = salesmanUserPriceManager
            .getMultipleOffersPrices(
              Seq(offer),
              Seq(AutoruProduct.PackageVip),
              applyMoneyFeature = false,
              isNewDraft = false
            )
            .futureValue
          pricesMap.toList.head._2.size shouldBe 1
      }
    }

    "fail getActivationPrice if response from salesman is empty" in {
      forAll(OfferGen) { offer =>
        implicit val request = generateReq(UserRef.parse(offer.getUserRef))

        when(passportClient.getUserModeration(?)(?)).thenReturnF(notReseller)
        when(salesmanUserClient.getMultipleOffersPrices(?, ?, ?)(?))
          .thenReturnF(Iterable.empty[ApiModel.ProductPrices])
        val res = salesmanUserPriceManager.getActivationPrice(offer)(request).failed
        res.value.get.get shouldBe an[EmptyResponseException]
      }
    }

    "fail getActivationPrice if there is no product prices in response" in {
      forAll(OfferGen) { offer =>
        implicit val request = generateReq(UserRef.parse(offer.getUserRef))
        val productPrices = ApiModel.ProductPrices.newBuilder.build
        when(passportClient.getUserModeration(?)(?)).thenReturnF(notReseller)
        when(salesmanUserClient.getMultipleOffersPrices(?, ?, ?)(?))
          .thenReturnF(Iterable(productPrices))
        val res = salesmanUserPriceManager.getActivationPrice(offer).failed

        res.value.get.get shouldBe an[NoPlacementPriceException]
      }
    }

    "fail getActivationPrice if there is no placement price in response" in {
      forAll(OfferGen, ProductPriceNotPlacementGen) { (offer, productPrice) =>
        implicit val request = generateReq(UserRef.parse(offer.getUserRef))
        val productPrices = ApiModel.ProductPrices.newBuilder
          .addProductPrices(productPrice)
          .build
        when(passportClient.getUserModeration(?)(?)).thenReturnF(notReseller)
        when(salesmanUserClient.getMultipleOffersPrices(?, ?, ?)(?))
          .thenReturnF(Iterable(productPrices))
        val res = salesmanUserPriceManager.getActivationPrice(offer).failed
        res.value.get.get shouldBe an[NoPlacementPriceException]
      }
    }

    "not fail getActivationPrice if there is placement price in salesman response" in {
      forAll(OfferGen, PlacementProductPriceGen) { (offer, placementPrice) =>
        implicit val request = generateReq(UserRef.parse(offer.getUserRef))
        val productPrices = ApiModel.ProductPrices.newBuilder
          .addProductPrices(placementPrice)
          .build
        when(passportClient.getUserModeration(?)(?)).thenReturnF(notReseller)
        when(salesmanUserClient.getMultipleOffersPrices(?, ?, ?)(?))
          .thenReturnF(Iterable(productPrices))
        val res = salesmanUserPriceManager.getActivationPrice(offer).value.get.get
        res.value shouldBe placementPrice.getPrice.getEffectivePrice / 100
        res.paidReason shouldBe Some(UserPriceConverter.toPaidReason(placementPrice.getPaymentReason))
        res.promocodeId shouldBe Some(placementPrice.getPrice.getModifier.getPromocoderFeature.getId)
          .filter(_.nonEmpty)
      }
    }

  }
}
