package ru.yandex.realty.seller.service.impl

import org.junit.runner.RunWith
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.abram.proto.api.ability._
import ru.yandex.realty.clients.abram.AbramClient
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.generator.Generators
import ru.yandex.realty.model.exception.NeedAuthenticationException
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.offer.{PaymentType, RentTime, _}
import ru.yandex.realty.model.user.{PassportUser, UserRef}
import ru.yandex.realty.promocoder.model.PromocoderGenerators
import ru.yandex.realty.promocoder.{FeaturesReceived, PromocoderAsyncClient, RetrieveFeaturesResponse}
import ru.yandex.realty.proto.Area
import ru.yandex.realty.proto.offer.vos.Offer
import ru.yandex.realty.proto.offer.{CampaignType, Product}
import ru.yandex.realty.proto.offer.vos.Offer.{
  Location,
  OfferInBuilding,
  RentOffer,
  VasUnavailableReason,
  VosOfferSource
}
import ru.yandex.realty.proto.offer.vos.OfferResponse.{VosOfferListResponse, VosOfferResponse}
import ru.yandex.realty.proto.offer.{
  OfferCategory,
  Photo,
  VasUnavailableReasonEnum,
  OfferType => ProtoOfferType,
  PaymentType => ProtoPaymentTime,
  RentTime => ProtoRentTime
}
import ru.yandex.realty.proto.unified.vos.offer.Publishing
import ru.yandex.realty.proto.unified.vos.offer.Publishing.ShowStatus
import ru.yandex.realty.seller.exceptions.OfferLocationUnknownException
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{
  BillingContext,
  Contexts,
  DisabledProductContext,
  MoneyPromocodePriceModifier,
  OfferTarget,
  ValidContexts,
  VasPromocodePriceModifier,
  ProductTypes => SellerProductTypes
}
import ru.yandex.realty.seller.model.price.ProductAvailable
import ru.yandex.realty.seller.model.product.converters.CampaignTypeConverter
import ru.yandex.realty.seller.service.PriceService.GetContextRequest
import ru.yandex.realty.seller.service.builders.PromocodeHelper
import ru.yandex.realty.seller.service.util.ProductPriceConverter.DEFAULT_AREA_UNIT
import ru.yandex.vertis.protobuf.ProtoInstanceProvider
import ru.yandex.realty.tracing.Traced
import PriceServiceImplSpec._
import org.mockito.Mockito
import ru.yandex.realty.clients.cadastr.CadastrClient
import ru.yandex.realty.context.v2.placement.{PlacementDiscountProvider, PlacementDiscountStorage}
import ru.yandex.realty.crm.common.ClientPlacementDiscount
import ru.yandex.realty.persistence.PartnerId
import ru.yandex.realty.seller.dao.TariffDao
import ru.yandex.realty.seller.model.ProductType
import ru.yandex.realty.seller.model.product._
import ru.yandex.realty.seller.service.CampaignHeaderService

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class PriceServiceImplSpec
  extends AsyncSpecBase
  with PropertyChecks
  with ProtobufMessageGenerators
  with SellerModelGenerators
  with PromocoderGenerators
  with ProtoInstanceProvider
  with ShrinkLowPriority {

  private val vosClient: VosClientNG = mock[VosClientNG]
  private val cadastrClient: CadastrClient = mock[CadastrClient]
  private val promocoderClient: PromocoderAsyncClient = mock[PromocoderAsyncClient]
  private val abramClient: AbramClient = mock[AbramClient]
  private val placementDiscountProviderMock = Mockito.mock(classOf[PlacementDiscountProvider])
  private val tariffDao = mock[TariffDao]
  private val campaignHeaderService = mock[CampaignHeaderService]
  private val placementDiscountStorage = PlacementDiscountStorage(Map.empty[String, Seq[ClientPlacementDiscount]])
  Mockito.when(placementDiscountProviderMock.get()).thenReturn(placementDiscountStorage)
  private val abilitiesManager = new ProductAbilitiesManager(abramClient, placementDiscountProviderMock)
  private val service = new PriceServiceImpl(
    vosClient,
    cadastrClient,
    promocoderClient,
    abramClient,
    abilitiesManager,
    placementDiscountProviderMock,
    tariffDao,
    campaignHeaderService
  )
  implicit private val trace: Traced = Traced.empty

  private val validLocationGen = for {
    l <- generate[Location]()
    rgid <- posNum[Long]
  } yield l.toBuilder.setRgid(rgid).build()

  private val respGen = generate[VosOfferResponse](depth = 10)

  private val validOfferRespGen = for {
    resp <- respGen
    location <- validLocationGen
    partnerId <- posNum[Long]
  } yield {
    val b = resp.toBuilder
      .setUnifiedLocation(location)
      .clearVasUnavailableReason()
      .clearState
      .clearPrevState()

    b.getContentBuilder.addPhoto(Photo.newBuilder().setUrl("").build())

    b.getContentBuilder
      .setPartnerId(partnerId)
      .setShowStatus(ShowStatus.PUBLISHED)

    b.getContentBuilder.getPlacementBuilder
      .setQuotaApplied(false)
      .setMeetsPaymentCriteria(false)

    b.build()
  }

  "PriceServiceImpl" should {

    "getPrices" should {
      "fail on non-passport UserRef" in {
        forAll(anonymousUserRefGen, offerTargetGen) { (userRef, target) =>
          interceptCause[NeedAuthenticationException] {
            service.getPrices(userRef, target, None).futureValue
          }
        }
      }

      "fail on offer without rgid" in {
        forAll(passportUserGen, offerTargetGen, validOfferRespGen) { (userRef, target, resp) =>
          val rb = resp.toBuilder.clearUnifiedLocation()
          rb.getContentBuilder.clearLocation()
          val response = rb.build()

          mockGetVosOffer()
          mockVosClientBatch(userRef, Seq(target), Seq(response))
          mockCampaignHeadersService

          interceptCause[OfferLocationUnknownException] {
            service.getPrices(userRef, target, None).futureValue
          }
        }
      }

      //FIXME
      /* "request prices for offer with valid location" in {
        forAll(passportUserGen, validOfferRespGen, validLocationGen) { (userRef, resp, location) =>
          val offerResponse = resp.toBuilder.setUnifiedLocation(location).build()

          val target = OfferTarget(offerResponse.getContent.getId)

          mockVosClientForGetPrice(userRef, target, offerResponse)
          mockPromocoderFeatures(userRef, FeaturesReceived(Seq.empty))

          val priceRequest = prepareRequest(location, offerResponse, target)
          (
            (
              request: ProductPriceBatchRequest,
              traced: Traced
            ) =>
              abramClient
                .getProductPriceBatch(
                  request: ProductPriceBatchRequest
                )(traced)
            )
            .expects(ProductPriceBatchRequest.newBuilder().addPriceRequest(priceRequest).build(), *)
            .returning(
              Future(
                ProductBatchPriceResponse
                  .newBuilder()
                  .addPriceResponse(ProductPriceResponse.newBuilder().setUuid(target.offerId))
                  .build()
              )
            )

          service.getPrices(userRef, target, None).futureValue
        }
      }*/

      "get prices for available products" in {
        forAll(
          passportUserGen,
          validOfferRespGen,
          validLocationGen,
          Generators.allProductPriceGen
        ) { (userRef, resp, location, _) =>
          val target = OfferTarget(resp.getContent.getId)
          val response = resp.toBuilder
            .setUnifiedLocation(location)
            .clearVasUnavailableReason()
            .build()

          mockVosClientForGetPrice(userRef, target, response)
          mockGetVosOffer()
          mockPricesRetrieverBatch(VasPricesWithAllRequiredProducts, Seq(target))
          mockPromocoderFeatures(userRef, FeaturesReceived(Seq()))
          mockCampaignHeadersService

          val result = service.getPrices(userRef, target, None).futureValue
          result.size shouldBe VasPricesWithAllRequiredProducts.size
          result.foreach {
            case ValidContexts(Contexts(_, productType, priceContext, productContext, _, _)) =>
              val v = VasPricesWithAllRequiredProducts
                .find(p => CampaignTypeConverter.protoProductTypesToSeller(p.getProductType) == productType)
                .getOrElse(throw new IllegalStateException(s"No vasPrice for $productType"))

              val product = ProductAvailable(
                productType,
                priceContext.basePrice,
                priceContext.basePrice,
                0,
                Iterable.empty,
                Iterable.empty
              )

              checkProductAvailable(response, product, v, productContext.duration)
          }
        }
      }

      "respect unavailable products" in {
        forAll(
          passportUserGen,
          validOfferRespGen,
          validLocationGen,
          Generators.allProductPriceGen
        ) { (userRef, resp, location, productPrices) =>
          val target = OfferTarget(resp.getContent.getId)
          val reasons = VasPricesWithAllRequiredProducts.map { p =>
            VasUnavailableReason
              .newBuilder()
              .setCampaignValue(CampaignTypeConverter.productTypesToOfferCampaignType(p.getProductType).value)
              .addReason(VasUnavailableReasonEnum.VST_WRONG_STATUS)
              .addReason(VasUnavailableReasonEnum.VST_NO_PHOTO)
              .build()
          }
          val response = resp.toBuilder
            .setUnifiedLocation(location)
            .clearVasUnavailableReason()
            .addAllVasUnavailableReason(reasons.asJava)
            .build()

          mockVosClientBatch(userRef, Seq(target), Seq(response))
          mockGetVosOffer()
          mockPromocoderFeatures(userRef, FeaturesReceived(Seq()))
          mockPricesRetrieverBatch(VasPricesWithAllRequiredProducts, Seq(target))
          mockCampaignHeadersService

          val result = service.getPrices(userRef, target, None).futureValue
          result.size shouldBe VasPricesWithAllRequiredProducts.size
          result.foreach {
            case DisabledProductContext(_, _, r) =>
              (r should contain).allOf(VasUnavailableReasonEnum.VST_WRONG_STATUS, VasUnavailableReasonEnum.VST_NO_PHOTO)
          }
        }
      }

      "fill products with required products, marked as disabled on their absence" in {
        forAll(
          passportUserGen,
          validOfferRespGen,
          validLocationGen
        ) { (userRef, resp, location) =>
          val target = OfferTarget(resp.getContent.getId)
          val response = resp.toBuilder
            .setUnifiedLocation(location)
            .setContent(
              VosOfferSource
                .newBuilder()
                .setId(target.offerId)
                .setShowStatus(Publishing.ShowStatus.PUBLISHED)
                .addProducts(
                  Product.newBuilder().setId("1").setActive(true).setType(CampaignType.CAMPAIGN_TYPE_RAISING).build()
                )
                .addProducts(
                  Product.newBuilder().setId("2").setActive(true).setType(CampaignType.CAMPAIGN_TYPE_PREMIUM).build()
                )
                .addProducts(
                  Product.newBuilder().setId("3").setActive(true).setType(CampaignType.CAMPAIGN_TYPE_PROMOTION).build()
                )
                .setOfferInBuilding(
                  OfferInBuilding
                    .newBuilder()
                    .setLivingOffer(
                      Offer.LivingOffer
                        .newBuilder()
                        .setRoomsOffer(Offer.RoomsOffer.newBuilder().addFloor(1).build())
                        .build()
                    )
                    .build()
                )
                .setRentOffer(RentOffer.getDefaultInstance)
                .build()
            )
            .clearVasUnavailableReason()
            .build()

          mockVosClientForGetPrice(userRef, target, response)
          mockGetVosOffer()
          mockPromocoderFeatures(userRef, FeaturesReceived(Seq()))
          mockCampaignHeadersService
          mockPricesRetrieverBatch(
            List[ProductPrice](
              ProductPrice
                .newBuilder()
                .setProductType(ru.yandex.realty.proto.seller.ProductTypes.PRODUCT_TYPE_RAISING)
                .setBasePrice(100)
                .setPeriodDays(1)
                .build(),
              ProductPrice
                .newBuilder()
                .setProductType(ru.yandex.realty.proto.seller.ProductTypes.PRODUCT_TYPE_PREMIUM)
                .setBasePrice(100)
                .setPeriodDays(1)
                .build(),
              ProductPrice
                .newBuilder()
                .setProductType(ru.yandex.realty.proto.seller.ProductTypes.PRODUCT_TYPE_PROMOTION)
                .setBasePrice(100)
                .setPeriodDays(1)
                .build(),
              ProductPrice
                .newBuilder()
                .setProductType(ru.yandex.realty.proto.seller.ProductTypes.PRODUCT_TYPE_PLACEMENT)
                .setBasePrice(100)
                .setPeriodDays(1)
                .build()
            ),
            Seq(target)
          )

          val result = service.getPrices(userRef, target, None).futureValue
          result.size shouldBe 5
          result should contain
          DisabledProductContext(
            OfferTarget(""),
            ProductTypes.PackageTurbo,
            Set(VasUnavailableReasonEnum.VST_CALCULATION_ERROR)
          )
        }
      }
    }

    "getContexts" should {
      "create correct contexts without promocodes" in {
        forAll(passportUserGen, Gen.chooseNum(2, 4), Generators.allProductPriceGen) { (userRef, count, vasPrices) =>
          val responses = listUnique(count, count, validOfferRespGen)(_.getContent.getId).next
          val targets = responses.map(r => OfferTarget(r.getContent.getId))
          val products = vasPrices.map(price => CampaignTypeConverter.protoProductTypesToSeller(price.getProductType)) //productTypeGen.next(count)

          mockVosClientBatch(userRef, targets, responses)
          mockGetVosOffer()
          mockPricesRetrieverBatch(vasPrices, targets)
          mockPromocoderFeatures(userRef, FeaturesReceived(Seq.empty))
          mockCampaignHeadersService

          val t2p = targets.zip(products).toMap
          val req = t2p.map {
            case (target, product) => GetContextRequest(target, product)
          }
          val contextsList = service.getContexts(userRef, req, extendedUserType = None).futureValue

          contextsList should have size count
          contextsList.foreach { contexts =>
            val target = contexts.target.asInstanceOf[OfferTarget]
            val product = t2p(target)
            val response = responses.find(_.getContent.getId == target.offerId).get

            contexts.productType shouldBe product
            val paymentType = PriceRetrieverHelper.getPaymentType(response)

            val partnerId = response.getContent.getPartnerId
            contexts.billingContext should matchPattern {
              case Some(BillingContext(Some(`partnerId`), None, None, None, None))
                  if paymentType == PaymentType.JURIDICAL_PERSON =>
              case None if paymentType == PaymentType.NATURAL_PERSON =>
            }

            val price = vasPrices
              .find(p => CampaignTypeConverter.protoProductTypesToSeller(p.getProductType) == product)
              .getOrElse(throw new IllegalStateException(s"No vasPrice for $product"))

            contexts.productContext.paymentType shouldBe Some(paymentType)
            contexts.productContext.duration.length shouldBe price.getPeriodDays

            contexts.priceContext.basePrice shouldBe contexts.priceContext.effectivePrice
            contexts.priceContext.modifiers shouldBe empty
          }
        }
      }

      "use vas promo code features" in {
        forAll(
          passportUserGen,
          Gen.chooseNum(2, 4),
          Generators.allProductPriceGen,
          list(20, 30, featureInstanceGen(PromocodeHelper.getUsefulFeatureTags - PromocodeHelper.moneyTag))
        ) { (userRef, count, vasPrices, features) =>
          val responses = listUnique(count, count, validOfferRespGen)(_.getContent.getId).next
          val targets = responses.map(r => OfferTarget(r.getContent.getId))
          val products = productTypeGen.next(count)

          mockVosClientBatch(userRef, targets, responses)
          mockGetVosOffer()
          mockPricesRetrieverBatch(vasPrices, targets)
          mockPromocoderFeatures(userRef, FeaturesReceived(features))
          mockCampaignHeadersService
          val t2p = targets.zip(products).toMap
          val req = t2p.map {
            case (target, product) => GetContextRequest(target, product)
          }
          val contextsList = service.getContexts(userRef, req, extendedUserType = None).futureValue

          contextsList should have size count
          contextsList.foreach { contexts =>
            val target = contexts.target.asInstanceOf[OfferTarget]
            val product = t2p(target)

            contexts.target shouldBe target
            contexts.productType shouldBe product

            val priceContext = contexts.priceContext

            priceContext.basePrice should be >= 0L
            val modifiers = priceContext.modifiers.toList
            if (modifiers.nonEmpty) {
              priceContext.effectivePrice shouldBe 0
              modifiers should have size 1
              modifiers.head match {
                case VasPromocodePriceModifier(id, _, cnt) =>
                  features.map(_.id).toSet should contain(id)
                  cnt shouldBe 1
              }
            }
          }
        }
      }

      "use money promo code features" in {
        forAll(
          passportUserGen,
          Gen.chooseNum(2, 4),
          Generators.allProductPriceGen,
          list(10, 15, featureInstanceGen(Set(PromocodeHelper.moneyTag)))
        ) { (userRef, count, vasPrices, features) =>
          val responses = listUnique(count, count, validOfferRespGen)(_.getContent.getId).next
          val targets = responses.map(r => OfferTarget(r.getContent.getId))
          val products = productTypeGen.next(count)

          mockVosClientBatch(userRef, targets, responses)
          mockGetVosOffer()
          mockPricesRetrieverBatch(vasPrices, targets)
          mockPromocoderFeatures(userRef, FeaturesReceived(features))
          mockCampaignHeadersService

          val t2p = targets.zip(products).toMap
          val req = t2p.map {
            case (target, product) => GetContextRequest(target, product)
          }
          val contextsList = service.getContexts(userRef, req, extendedUserType = None).futureValue

          var totalDelta = 0L
          contextsList should have size count
          contextsList.foreach { contexts =>
            val target = contexts.target.asInstanceOf[OfferTarget]
            val product = t2p(target)

            contexts.target shouldBe target
            contexts.productType shouldBe product

            val priceContext = contexts.priceContext

            priceContext.basePrice should be >= 0L
            priceContext.effectivePrice should be >= 0L
            val delta = priceContext.basePrice - priceContext.effectivePrice
            val modifiers = priceContext.modifiers.toList
            val promoCount = modifiers.map {
              case MoneyPromocodePriceModifier(id, sum) =>
                features.map(_.id).toSet should contain(id)
                sum should be > 0
                sum
            }.sum

            delta shouldBe promoCount * 100

            totalDelta += delta
          }
          val totalPromo = features.map(_.count).sum
          totalDelta should be <= totalPromo * 100
        }
      }

    }
  }

  private def prepareRequest(
    location: Location,
    response: VosOfferResponse,
    target: OfferTarget
  ): ProductPriceRequest = {
    val priceRequest = ProductPriceRequest
      .newBuilder()
      .setRgid(location.getRgid.toInt)
      .setOfferType(ProtoOfferType.forNumber(PriceRetrieverHelper.getOfferType(response).value()))
      .setCategoryType(OfferCategory.forNumber(PriceRetrieverHelper.getCategoryType(response).value()))
      .setRentTime(
        ProtoRentTime.forNumber(
          PriceRetrieverHelper
            .getRentTime(response)
            .getOrElse(RentTime.UNKNOWN)
            .value()
        )
      )
      .setPaymentType(ProtoPaymentTime.forNumber(PriceRetrieverHelper.getPaymentType(response).value()))
      .setPrice(PriceRetrieverHelper.getPrice(response))
      .setArea(
        Area.newBuilder().setValue(PriceRetrieverHelper.getFullArea(response)).setUnit(DEFAULT_AREA_UNIT)
      )
      .setUuid(target.offerId)
      .build()

    priceRequest
  }

  private def mockVosClientBatch(
    userRef: PassportUser,
    target: Iterable[OfferTarget],
    resp: Iterable[VosOfferResponse]
  ): Unit = {
    (vosClient
      .getOffers(_: String, _: Iterable[String])(_: Traced))
      .expects(userRef.uid.toString, target.map(_.offerId).toSet, *)
      .returning(Future.successful(VosOfferListResponse.newBuilder().addAllOffers(resp.asJava).build()))
  }

  private def mockGetVosOffer(): Unit = {
    (vosClient
      .getOffer(_: String, _: String)(_: Traced))
      .expects(*, *, *)
      .returning(Future.successful(None))
  }

  private def mockVosClientForGetPrice(userRef: PassportUser, target: OfferTarget, resp: VosOfferResponse): Unit = {
    mockVosClientBatch(userRef, Seq(target), Seq(resp))
  }

  private def mockPricesRetrieverBatch(prices: Seq[ProductPrice], offers: Seq[OfferTarget]): Unit = {
    (
      (
        request: ProductPriceBatchRequest,
        traced: Traced
      ) =>
        abramClient
          .getProductPriceBatch(
            request: ProductPriceBatchRequest
          )(traced)
      )
      .expects(*, *)
      .returning(
        Future(
          ProductBatchPriceResponse.newBuilder
            .addAllPriceResponse(
              offers
                .map(
                  offer =>
                    ProductPriceResponse.newBuilder
                      .setUuid(offer.offerId)
                      .addAllProducts(prices.asJava)
                      .build()
                )
                .asJava
            )
            .build()
        )
      )
  }

  private def mockPromocoderFeatures(userRef: PassportUser, response: RetrieveFeaturesResponse) = {
    (promocoderClient
      .getFeatures(_: String)(_: Traced))
      .expects(userRef.uid.toString, *)
      .returning(Future.successful(response))
  }

  private def mockCampaignHeadersService = {
    (campaignHeaderService
      .getHeader(_: UserRef, _: Option[PartnerId], _: ProductType))
      .expects(*, *, *)
      .returning(None)
  }

  private def checkProductAvailable(
    resp: VosOfferResponse,
    p: ProductAvailable,
    v: ProductPrice,
    duration: Duration
  ) = {
    val paymentType = PriceRetrieverHelper.getPaymentType(resp)
    val activeProducts = PriceRetrieverHelper.getActiveProducts(resp)

    val correctPrice =
      if (paymentType == PaymentType.JURIDICAL_PERSON &&
        p.productType == SellerProductTypes.Promotion &&
        activeProducts.contains(SellerProductTypes.Raising)) {
        0
      } else {
        v.getBasePrice
      }

    duration shouldBe v.getPeriodDays.days
    p.price shouldBe (correctPrice * 100)
  }
}

object PriceServiceImplSpec {

  val VasPricesWithAllRequiredProducts: List[ProductPrice] = List(
    ProductPrice
      .newBuilder()
      .setProductType(ru.yandex.realty.proto.seller.ProductTypes.PRODUCT_TYPE_RAISING)
      .setBasePrice(100)
      .setPeriodDays(1)
      .build(),
    ProductPrice
      .newBuilder()
      .setProductType(ru.yandex.realty.proto.seller.ProductTypes.PRODUCT_TYPE_PREMIUM)
      .setBasePrice(100)
      .setPeriodDays(1)
      .build(),
    ProductPrice
      .newBuilder()
      .setProductType(ru.yandex.realty.proto.seller.ProductTypes.PRODUCT_TYPE_PROMOTION)
      .setBasePrice(100)
      .setPeriodDays(1)
      .build(),
    ProductPrice
      .newBuilder()
      .setProductType(ru.yandex.realty.proto.seller.ProductTypes.PRODUCT_TYPE_PACKAGE_TURBO)
      .setBasePrice(100)
      .setPeriodDays(1)
      .build(),
    ProductPrice
      .newBuilder()
      .setProductType(ru.yandex.realty.proto.seller.ProductTypes.PRODUCT_TYPE_PLACEMENT)
      .setBasePrice(100)
      .setPeriodDays(1)
      .build()
  )
}
