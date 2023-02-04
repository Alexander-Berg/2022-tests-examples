package ru.yandex.realty.managers.events

import org.junit.runner.RunWith
import org.scalatest.PrivateMethodTester
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.abram.proto.api.call.prices.{CallPrice, CallPriceRequest, CallPricesRequest, CallPricesResponse}
import ru.yandex.realty.clients.abram.AbramClient
import ru.yandex.realty.clients.seller.SellerClient
import ru.yandex.realty.events.Event
import ru.yandex.realty.events.OfferServiceNamespace.OfferService
import ru.yandex.realty.persistence.OfferId
import ru.yandex.realty.proto.offer.OfferCategory
import ru.yandex.realty.proto.seller.{ProductTypes, PurchasedProductStatus}
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.unified.offer.vas.CommonTuzInfo.TeleponyTag
import ru.yandex.realty.proto.unified.offer.vas.{CommonTuzInfo, ExtendedTariffInfo, VasInfo}
import ru.yandex.realty.seller.api.SellerApi.ProductSearchRequest
import ru.yandex.realty.seller.proto.api.price.{PriceAvailable, PriceContext}
import ru.yandex.realty.seller.proto.api.products.{Product, ProductList, ProductSearchResponse}
import ru.yandex.realty.seller.proto.api.purchase.{OfferTarget, PurchaseProduct, PurchaseTarget}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.protobuf.BasicEnumProtoFormats.CategoryTypeProtoFormat

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CallPricesEventManagerSpec extends AsyncSpecBase with PrivateMethodTester {

  private val abramClient = mock[AbramClient]
  private val sellerClient = mock[SellerClient]
  private val manager = new CallPricesEventManagerImpl(abramClient, sellerClient)

  "CallPricesEventManager" should {
    "getCallPrices" in {
      val basePrice = 5
      (abramClient
        .getCallPrices(_: CallPricesRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(
          Future.successful(
            CallPricesResponse
              .newBuilder()
              .addPrices(CallPrice.newBuilder().setBasePrice(basePrice))
              .build()
          )
        )

      val offerIdWithTuz = new OfferId("offerIdWithTuz")
      val unifiedOfferWithTeleponyTags = UnifiedOffer
        .newBuilder()
        .setVasInfo(
          VasInfo
            .newBuilder()
            .setTuzExtended(
              ExtendedTariffInfo
                .newBuilder()
                .setCommonTuzInfo(
                  CommonTuzInfo
                    .newBuilder()
                    .addTeleponyParams(
                      TeleponyTag.newBuilder().setKey("tuzParam").setValue("1")
                    )
                )
            )
        )
        .build()

      val eventBuilder = Event.newBuilder()
      eventBuilder.getObjectInfoBuilder.getOfferInfoBuilder.setOfferId(offerIdWithTuz)
      val eventWithTuz = eventBuilder.build()
      val params: Map[Event, UnifiedOffer] = Map(
        eventWithTuz -> unifiedOfferWithTeleponyTags,
        Event.getDefaultInstance -> UnifiedOffer.getDefaultInstance
      )
      val event2Request = buildCallPriceRequests(params)
      val resultMap = manager.getCallPrices(event2Request.values.toSeq)(Traced.empty).futureValue

      resultMap.size shouldBe 1
      resultMap(event2Request(eventWithTuz)).getBasePrice shouldBe basePrice
    }

    "getCallPrices for two events for same offer" in {
      val teleponyTag = TeleponyTag.newBuilder().setKey("tuzParam").setValue("1").build()
      val buildCallPriceRequest: PrivateMethod[Option[CallPriceRequest]] =
        PrivateMethod[Option[CallPriceRequest]]('buildCallPriceRequest)
      val callPriceRequest = manager.invokePrivate(buildCallPriceRequest(Seq(teleponyTag), "TUZ")).get
      val basePrice = 5
      (abramClient
        .getCallPrices(_: CallPricesRequest)(_: Traced))
        .expects(where { (r: CallPricesRequest, _) =>
          r.getPriceRequests(0).getTeleponyTag == callPriceRequest.getTeleponyTag &&
          r.getPriceRequests(0).getProduct == callPriceRequest.getProduct
        })
        .once()
        .returning(
          Future.successful(
            CallPricesResponse
              .newBuilder()
              .addPrices(CallPrice.newBuilder().setBasePrice(basePrice))
              .build()
          )
        )

      val offerIdWithTuz = new OfferId("sameOfferId")
      val unifiedOfferWithTeleponyTags = UnifiedOffer
        .newBuilder()
        .setVasInfo(
          VasInfo
            .newBuilder()
            .setTuzExtended(
              ExtendedTariffInfo
                .newBuilder()
                .setCommonTuzInfo(
                  CommonTuzInfo
                    .newBuilder()
                    .addTeleponyParams(
                      teleponyTag
                    )
                )
            )
        )
        .build()

      val eventWithPrimarySaleBuilder = Event.newBuilder()
      eventWithPrimarySaleBuilder.getObjectInfoBuilder.getOfferInfoBuilder
        .setOfferId(offerIdWithTuz)
        .setOfferCategoryField(OfferCategory.APARTMENT)
        .setRevoked(true)
        .getSellOfferBuilder
        .setPrimarySale(true)
      val eventWithPrimarySale = eventWithPrimarySaleBuilder.build()

      val eventBuilder = Event.newBuilder()
      eventBuilder.getObjectInfoBuilder.getOfferInfoBuilder
        .setOfferId(offerIdWithTuz)
        .setOfferCategoryField(OfferCategory.APARTMENT)
        .getSellOfferBuilder
        .setPrimarySale(false)
      val event = eventBuilder.build()

      val params: Map[Event, UnifiedOffer] = Map(
        eventWithPrimarySale -> unifiedOfferWithTeleponyTags,
        event -> unifiedOfferWithTeleponyTags
      )
      val event2Request = buildCallPriceRequests(params)
      val resultMap = manager.getCallPrices(event2Request.values.toSeq)(Traced.empty).futureValue

      resultMap.size shouldBe 1
      resultMap(event2Request(event)).getBasePrice shouldBe basePrice
    }

    "getPurchasedVasPrices" in {
      val basePrice = 5
      val effectivePrice = 3
      val userUid = "123532"
      val userUid3 = "123123532"
      val offerId = "4356123532"
      val secondOfferId = "0123456789"
      val thirdOfferId = "98732674"
      val request = ProductSearchRequest
        .newBuilder()
        .addAllOfferId(Seq(offerId, secondOfferId, thirdOfferId).asJava)
        .addStatus(PurchasedProductStatus.PURCHASED_PRODUCT_STATUS_ACTIVE)
        .build()
      val target: PurchaseTarget =
        PurchaseTarget.newBuilder().setOfferTarget(OfferTarget.newBuilder().setOfferId(offerId)).build()
      val thirdTarget: PurchaseTarget =
        PurchaseTarget.newBuilder().setOfferTarget(OfferTarget.newBuilder().setOfferId(thirdOfferId)).build()

      mockSellerResponse(basePrice, effectivePrice, request, target, thirdTarget)

      val eventBuilder = Event.newBuilder()
      eventBuilder.getObjectInfoBuilder.getOfferInfoBuilder.setOfferId(offerId)
      eventBuilder.getObjectInfoBuilder.getOfferInfoBuilder.getOfferServicesInfoBuilder
        .addOfferServices(OfferService.RAISING)
      val event = eventBuilder.build()
      val eventBuilder2 = Event.newBuilder()
      eventBuilder2.getObjectInfoBuilder.getOfferInfoBuilder.setOfferId(secondOfferId)
      val secondEvent = eventBuilder2.build()
      val eventBuilder3 = Event.newBuilder()
      eventBuilder3.getObjectInfoBuilder.getOfferInfoBuilder.setOfferId(thirdOfferId)
      val thirdEvent = eventBuilder3.build()

      val params: Map[Event, UnifiedOffer] = Map(
        event -> UnifiedOffer.newBuilder().setOfferId(offerId).setUserRef(userUid).build(),
        secondEvent -> UnifiedOffer.newBuilder().setOfferId(secondOfferId).setUserRef(userUid).build(),
        thirdEvent -> UnifiedOffer.newBuilder().setOfferId(thirdOfferId).setUserRef(userUid3).build()
      )
      val resultMap = manager.getPurchasedVasPrices(params.values.toSeq)(Traced.empty).futureValue

      resultMap.size shouldBe 2
      resultMap.get(params(secondEvent)) shouldBe empty

      val prices = resultMap(params(event))
      prices.size shouldBe 1
      val price = prices.iterator.next()
      price.getBasePrice shouldBe basePrice
      price.getEffectivePrice shouldBe effectivePrice
      price.getProductType shouldBe ProductTypes.PRODUCT_TYPE_RAISING

      val prices3 = resultMap(params(thirdEvent))
      prices3.size shouldBe 1
      val price3 = prices3.iterator.next()
      price3.getBasePrice shouldBe basePrice * 2
      price3.getEffectivePrice shouldBe effectivePrice * 2
      price3.getProductType shouldBe ProductTypes.PRODUCT_TYPE_PROMOTION
    }
  }

  private def buildCallPriceRequests(event2Offer: Map[Event, UnifiedOffer]): Map[Event, CallPriceRequest] = {
    val result = for {
      (event, offer) <- event2Offer
      primarySale = event.getObjectInfo.getOfferInfo.getSellOffer.getPrimarySale
      category = CategoryTypeProtoFormat.read(event.getObjectInfo.getOfferInfo.getOfferCategoryField)
      callPriceRequest <- manager.getCallPriceRequest(offer, primarySale, category)
    } yield event -> callPriceRequest
    result.toMap
  }

  private def mockSellerResponse(
    basePrice: Int,
    effectivePrice: Int,
    request: ProductSearchRequest,
    target: PurchaseTarget,
    thirdTarget: PurchaseTarget
  ): Unit = {
    val productBuilder = buildProduct(basePrice, effectivePrice, target, ProductTypes.PRODUCT_TYPE_RAISING)
    val productBuilder3 =
      buildProduct(basePrice * 2, effectivePrice * 2, thirdTarget, ProductTypes.PRODUCT_TYPE_PROMOTION)
    (sellerClient
      .getProductsList(_: ProductSearchRequest)(_: Traced))
      .expects(request, *)
      .once()
      .returning(
        Future.successful(
          ProductSearchResponse
            .newBuilder()
            .setProductList(ProductList.newBuilder().addProducts(productBuilder).addProducts(productBuilder3))
            .build()
        )
      )
  }

  private def buildProduct(basePrice: Int, effectivePrice: Int, target: PurchaseTarget, types: ProductTypes) = {
    Product
      .newBuilder()
      .setProduct(
        PurchaseProduct
          .newBuilder()
          .setTarget(target)
          .setType(types)
          .setStatus(PurchasedProductStatus.PURCHASED_PRODUCT_STATUS_ACTIVE)
          .setPrice(
            PriceContext
              .newBuilder()
              .setAvailablePrice(
                PriceAvailable.newBuilder().setBase(basePrice).setEffective(effectivePrice)
              )
          )
      )
  }
}
