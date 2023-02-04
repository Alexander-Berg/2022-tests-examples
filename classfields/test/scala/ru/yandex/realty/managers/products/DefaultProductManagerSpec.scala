package ru.yandex.realty.managers.products

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.blackbox.BlackboxClient
import ru.yandex.realty.clients.cadastr.CadastrClient
import ru.yandex.realty.clients.seller.{ProductFilter, SellerClient}
import ru.yandex.realty.clients.vos.VosClient
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.context.VasProductDictionaryStorage
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.managers.banker.BankerManager
import ru.yandex.realty.model.message.ExtDataSchema.ProductDictionaryRecord
import ru.yandex.realty.model.message.ExtDataSchema.ProductDictionaryRecord.{PaymentType, ProductType, TargetType}
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.promocoder.{FeaturesReceived, PromocoderAsyncClient}
import ru.yandex.realty.proto.offer.VasUnavailableReasonEnum
import ru.yandex.realty.proto.seller.Target.OfferTarget
import ru.yandex.realty.proto.seller.{ProductTypes, Target}
import ru.yandex.realty.seller.proto.api.price.{PriceAvailable, PriceContext}
import ru.yandex.realty.seller.proto.api.products.{
  CalculateProductPricesRequest,
  ProductList,
  ProductPricesResponse,
  ProductSearchResponse,
  TargetToProductPrice,
  TargetToProductPriceList
}
import ru.yandex.realty.seller.proto.api.renewals.internal.InternalRenewalState
import ru.yandex.realty.vos.model.user.{User, UserInfo}
import ru.yandex.realty.proto.offer.{PaymentType => ProtoPaymentType}
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class DefaultProductManagerSpec extends AsyncSpecBase with RequestAware {

  private val sellerClient = mock[SellerClient]
  private val cadastrClient = mock[CadastrClient]
  private val dictionary = mock[Provider[VasProductDictionaryStorage]]
  private val vosClient = mock[VosClientNG]
  private val blackboxClient = mock[BlackboxClient]
  private val oldVosClient = mock[VosClient]
  private val bankerManager = mock[BankerManager]
  private val promocoderClient = mock[PromocoderAsyncClient]

  val manager =
    new DefaultProductManager(
      dictionary,
      sellerClient,
      cadastrClient,
      vosClient,
      bankerManager,
      blackboxClient,
      oldVosClient,
      promocoderClient
    )

  private val storage = new VasProductDictionaryStorage(
    Set(
      ProductDictionaryRecord
        .newBuilder()
        .setDescription("PREMIUM")
        .setType(ProductType.PREMIUM)
        .setPaymentType(PaymentType.NATURAL_PERSON)
        .setTarget(TargetType.OFFER)
        .build(),
      ProductDictionaryRecord
        .newBuilder()
        .setDescription("RAISING")
        .setType(ProductType.RAISING)
        .setPaymentType(PaymentType.NATURAL_PERSON)
        .setTarget(TargetType.OFFER)
        .build()
    )
  )

  private val theFailure = Future.failed(new IllegalArgumentException("Error message"))

  "DefaultProductManager" should {

    "Returns CalculationPriceError if seller.calculate-price returns error " in {
      (promocoderClient
        .getFeatures(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(FeaturesReceived(Seq.empty)))

      (dictionary.get: () => VasProductDictionaryStorage).expects().returning(storage)

      (sellerClient
        .calculatePrices(_: String, _: CalculateProductPricesRequest)(_: Traced))
        .expects(*, *, *)
        .returning(theFailure)
      (sellerClient
        .getProducts(_: String, _: ProductFilter)(_: Traced))
        .expects(*, *, *)
        .returning(
          Future.successful(ProductSearchResponse.newBuilder().setProductList(ProductList.newBuilder()).build())
        )
      (sellerClient
        .getRenewals(_: String, _: Set[String])(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(InternalRenewalState.getDefaultInstance))

      val dst = User
        .newBuilder()
        .setId(1)
        .setUserInfo(
          UserInfo
            .newBuilder()
            .setPaymentType(ProtoPaymentType.PT_NATURAL_PERSON)
        )
        .build()
      val userRef = UserRef.passport(dst.getId)

      withRequestContext(userRef) { implicit r =>
        {
          val result = manager.getProductInfo(dst, "123456").futureValue
          result.isDefined should be(true)
          result.get.productInfo.getProductsList.size() should be(2)
          result.get.productInfo.getProductsList
            .get(0)
            .getPriceContext
            .getDisabledPrice
            .getReasonsList
            .get(0) should be(
            VasUnavailableReasonEnum.VST_CALCULATION_ERROR
          )
          result.get.productInfo.getProductsList
            .get(1)
            .getPriceContext
            .getDisabledPrice
            .getReasonsList
            .get(0) should be(
            VasUnavailableReasonEnum.VST_CALCULATION_ERROR
          )
          result.get.productInfo.getProductsList.asScala
            .exists(_.getProductType == ProductTypes.PRODUCT_TYPE_PREMIUM) should be(
            true
          )
          result.get.productInfo.getProductsList.asScala
            .exists(_.getProductType == ProductTypes.PRODUCT_TYPE_RAISING) should be(
            true
          )
        }
      }
    }

    "Returns ProductStatus is empty if seller.get-products returns error " in {
      (promocoderClient
        .getFeatures(_: String)(_: Traced))
        .expects(*, *)
        .returning(Future.successful(FeaturesReceived(Seq.empty)))

      (dictionary.get: () => VasProductDictionaryStorage).expects().returning(storage)
      val target = Target.newBuilder().setOffer(OfferTarget.newBuilder().setOfferId("123456").build()).build()
      val price1 = TargetToProductPrice
        .newBuilder()
        .setType(ProductTypes.PRODUCT_TYPE_PREMIUM)
        .setTarget(target)
        .setPrice(PriceContext.newBuilder().setAvailablePrice(PriceAvailable.newBuilder().setBase(12L).build()).build())
        .build()
      val price2 = TargetToProductPrice
        .newBuilder()
        .setType(ProductTypes.PRODUCT_TYPE_RAISING)
        .setPrice(PriceContext.newBuilder().setAvailablePrice(PriceAvailable.newBuilder().setBase(20L).build()).build())
        .setTarget(target)
        .build()

      (sellerClient
        .calculatePrices(_: String, _: CalculateProductPricesRequest)(_: Traced))
        .expects(*, *, *)
        .returning(
          Future.successful(
            ProductPricesResponse
              .newBuilder()
              .setPriceList(
                TargetToProductPriceList
                  .newBuilder()
                  .addPrices(price1)
                  .addPrices(price2)
                  .build()
              )
              .build()
          )
        )
      (sellerClient
        .getProducts(_: String, _: ProductFilter)(_: Traced))
        .expects(*, *, *)
        .returning(theFailure)
      (sellerClient
        .getRenewals(_: String, _: Set[String])(_: Traced))
        .expects(*, *, *)
        .returning(Future.successful(InternalRenewalState.getDefaultInstance))

      val dst = User
        .newBuilder()
        .setId(1)
        .setUserInfo(
          UserInfo
            .newBuilder()
            .setPaymentType(ProtoPaymentType.PT_NATURAL_PERSON)
        )
        .build()
      val userRef = UserRef.passport(dst.getId)

      withRequestContext(userRef) { implicit r =>
        {
          val result = manager.getProductInfo(dst, target.getOffer.getOfferId).futureValue
          result.isDefined should be(true)
          result.get.productInfo.getProductsList.size() should be(2)
          result.get.productInfo.getProductsList.get(0).getPriceContext.hasAvailablePrice should be(true)
          result.get.productInfo.getProductsList.get(1).getPriceContext.hasAvailablePrice should be(true)
          result.get.productInfo.getProductsList.asScala
            .exists(_.getProductType == ProductTypes.PRODUCT_TYPE_PREMIUM) should be(
            true
          )
          result.get.productInfo.getProductsList.asScala
            .exists(_.getProductType == ProductTypes.PRODUCT_TYPE_RAISING) should be(
            true
          )
        }
      }
    }
  }
}
