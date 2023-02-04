package ru.yandex.realty.seller.builders

import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.abram.Tariff
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.proto.seller.{ProductTypes => ProtoProductTypes}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{Contexts, OfferTarget, PriceContext, ProductContext, ProductTypes}
import ru.yandex.realty.seller.model.purchase.{PurchaseDeliveryStatuses, PurchaseStatuses}
import ru.yandex.realty.seller.proto.api.purchase.{
  PurchaseInitItem,
  PurchaseInitRequest,
  OfferTarget => ProtoOfferTarget
}
import ru.yandex.realty.seller.service.PriceService
import ru.yandex.realty.seller.service.PriceService.GetContextRequest
import ru.yandex.realty.seller.service.builders.impl.PurchaseWithProductsBuilderImpl
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.vos.model.user.ExtendedUserType
import ru.yandex.vertis.protobuf.ProtoInstanceProvider
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler

import scala.concurrent.duration.DurationInt

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class PurchaseWithProductsBuilderSpec
  extends AsyncSpecBase
  with PropertyChecks
  with SellerModelGenerators
  with RequestAware
  with OneInstancePerTest
  with ProtoInstanceProvider
  with ProtobufMessageGenerators {

  private val priceService = mock[PriceService]
  private val pwpBuilder = new PurchaseWithProductsBuilderImpl(priceService)

  "PurchaseWithProductsBuilder" should {

    "build correct purchaseWithProduct" in {
      val userRef = passportUserGen.next

      val premiumTarget = OfferTarget("111111")
      val contextsPremium = Contexts(
        premiumTarget,
        ProductTypes.Premium,
        PriceContext(2, 2, Seq.empty, Seq.empty),
        ProductContext(1.day, Some(PaymentType.NATURAL_PERSON)),
        None
      )

      val raisingTarget = OfferTarget("222222")
      val contextsRaising = Contexts(
        raisingTarget,
        ProductTypes.PackageRaising,
        PriceContext(1, 1, Seq.empty, Seq.empty),
        ProductContext(7.days, Some(PaymentType.NATURAL_PERSON)),
        None
      )

      (priceService
        .getContexts(
          _: UserRef,
          _: Iterable[GetContextRequest],
          _: Boolean,
          _: Option[ExtendedUserType],
          _: Boolean
        )(_: Traced))
        .expects(userRef, *, *, *, false, *)
        .noMoreThanTwice()
        .returningF(Seq(contextsPremium, contextsRaising))

      val request = PurchaseInitRequest
        .newBuilder()
        .addItem(
          PurchaseInitItem
            .newBuilder()
            .setProductType(ProtoProductTypes.PRODUCT_TYPE_PACKAGE_RAISING)
            .setOffer(ProtoOfferTarget.newBuilder().setOfferId(raisingTarget.offerId))
        )
        .addItem(
          PurchaseInitItem
            .newBuilder()
            .setProductType(ProtoProductTypes.PRODUCT_TYPE_PREMIUM)
            .setOffer(ProtoOfferTarget.newBuilder().setOfferId(premiumTarget.offerId))
        )
        .build()

      val result = pwpBuilder.buildInitialPurchase(userRef, request, extendedUserType = None).futureValue

      result.purchase.id.nonEmpty should be(true)
      result.purchase.status should be(PurchaseStatuses.New)
      result.purchase.deliveryStatus should be(PurchaseDeliveryStatuses.NoOp)
      result.purchase.owner should be(userRef)
      result.products.seq.size should be(2)
      val premiumProduct = result.products.find(_.product == ProductTypes.Premium)
      premiumProduct should not be None
      premiumProduct.map(_.product) should be(Some(ProductTypes.Premium))
      premiumProduct.flatMap(_.priceContext) should be(Some(contextsPremium.priceContext))
      premiumProduct.map(_.purchaseId.getOrElse("")) should be(Some(result.purchase.id))
      premiumProduct.map(_.context) should be(Some(contextsPremium.productContext))
      val raisingProduct = result.products.find(_.product == ProductTypes.PackageRaising)
      raisingProduct should not be None
      raisingProduct.map(_.product) should be(Some(ProductTypes.PackageRaising))
      raisingProduct.flatMap(_.priceContext) should be(Some(contextsRaising.priceContext))
      raisingProduct.map(_.purchaseId.getOrElse("")) should be(Some(result.purchase.id))
      raisingProduct.map(_.context) should be(Some(contextsRaising.productContext))
    }

    "throw Exception if PriceService throw Exception" in {
      val userRef = passportUserGen.next
      (priceService
        .getContexts(
          _: UserRef,
          _: Iterable[GetContextRequest],
          _: Boolean,
          _: Option[ExtendedUserType],
          _: Boolean
        )(_: Traced))
        .expects(userRef, *, *, *, false, *)
        .noMoreThanTwice()
        .throwingF(new Exception("Some Exception"))

      val request = PurchaseInitRequest
        .newBuilder()
        .addItem(
          PurchaseInitItem
            .newBuilder()
            .setProductType(ProtoProductTypes.PRODUCT_TYPE_PACKAGE_RAISING)
            .setOffer(ProtoOfferTarget.newBuilder().setOfferId("111111"))
        )
        .addItem(
          PurchaseInitItem
            .newBuilder()
            .setProductType(ProtoProductTypes.PRODUCT_TYPE_PREMIUM)
            .setOffer(ProtoOfferTarget.newBuilder().setOfferId("222222"))
        )
        .build()

      interceptCause[Exception] {
        pwpBuilder.buildInitialPurchase(userRef, request, extendedUserType = None).futureValue
      }
    }
  }

}
