package ru.yandex.vos2.autoru.services.salesman

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.OfferModel.OfferService
import ru.yandex.vos2.autoru.utils.converters.offerform.OfferFormConverter
import ru.yandex.vos2.model.SalesmanModelGenerator._
import ru.yandex.vos2.model.offer.OfferGenerator
import ru.yandex.vos2.model.user.UserGenerator
import org.scalatest.TryValues._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.salesman.model.user.ApiModel.{ProductPrice, ProductPrices}
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class PriceServiceSpec extends AnyFunSuite with ScalaCheckPropertyChecks with Matchers with MockitoSupport {

  val offerFormConverter = mock[OfferFormConverter]
  val salesmanUserClient = mock[SalesmanUserClient]

  val priceService =
    new PriceService(salesmanUserClient, offerFormConverter)

  val convertedOffer = ApiOfferModel.Offer.newBuilder.build

  implicit val traced = Traced.empty

  test("have correct price and promocodeId") {
    forAll(
      OfferGenerator.offerWithRequiredFields(OfferService.OFFER_AUTO),
      OfferGenerator.autoruOfferWithRequiredFields(),
      UserGenerator.UserGen,
      productPriceGen(priceWithFeature)
    ) { (offerWithRequiredFields, autoruOfferWithRequiredFields, user, productPrice) =>
      when(offerFormConverter.convert(?, ?, ?, ?)(?)).thenReturn(convertedOffer)
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?))
        .thenReturn(Success(toProductPricesList(productPrice)))

      val autoruOffer = autoruOfferWithRequiredFields
        .setSellerType(AutoruOffer.SellerType.PRIVATE)
        .build
      val offer = offerWithRequiredFields
        .setOfferAutoru(autoruOffer)
        .setUser(user)
        .build
      val res = priceService.getActivationPrice(offer).success.value
      res.price shouldBe productPrice.getPrice.getEffectivePrice
      res.duration.toSeconds shouldBe productPrice.getDuration.getSeconds
      res.optPromocode.get.featureId shouldBe productPrice.getPrice.getModifier.getPromocoderFeature.getId
      res.optPromocode.get.count shouldBe productPrice.getPrice.getModifier.getPromocoderFeature.getCount
    }
  }

  test("have empty promocodeId when there is no feature") {
    forAll(
      OfferGenerator.offerWithRequiredFields(OfferService.OFFER_AUTO),
      OfferGenerator.autoruOfferWithRequiredFields(),
      UserGenerator.UserGen,
      productPriceGen(priceWithoutFeature)
    ) { (offerWithRequiredFields, autoruOfferWithRequiredFields, user, productPrice) =>
      when(offerFormConverter.convert(?, ?, ?, ?)(?)).thenReturn(convertedOffer)
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?))
        .thenReturn(Success(toProductPricesList(productPrice)))

      val autoruOffer = autoruOfferWithRequiredFields
        .setSellerType(AutoruOffer.SellerType.PRIVATE)
        .build
      val offer = offerWithRequiredFields
        .setOfferAutoru(autoruOffer)
        .setUser(user)
        .build
      val res = priceService.getActivationPrice(offer).success.value
      res.optPromocode shouldBe None
    }

  }

  test("throw exception on empty salesman response") {
    forAll(
      OfferGenerator.offerWithRequiredFields(OfferService.OFFER_AUTO),
      OfferGenerator.autoruOfferWithRequiredFields(),
      UserGenerator.UserGen
    ) { (offerWithRequiredFields, autoruOfferWithRequiredFields, user) =>
      when(offerFormConverter.convert(?, ?, ?, ?)(?)).thenReturn(convertedOffer)
      when(salesmanUserClient.getOfferProductPrice(?, ?)(?))
        .thenReturn(Success(List()))

      val autoruOffer = autoruOfferWithRequiredFields
        .setSellerType(AutoruOffer.SellerType.PRIVATE)
        .build
      val offer = offerWithRequiredFields
        .setOfferAutoru(autoruOffer)
        .setUser(user)
        .build
      val res = priceService.getActivationPrice(offer).failed
      res.get shouldBe a[NoActivationPriceException]
    }
  }

  def withPromocodeId(productPrice: ProductPrice, id: String): ProductPrice = {
    val price = productPrice.getPrice
    val modifier = price.getModifier
    val newFeature = modifier.getPromocoderFeature.toBuilder.setId(id)
    val newModifier = modifier.toBuilder.setPromocoderFeature(newFeature)
    productPrice.toBuilder
      .setPrice(price.toBuilder.setModifier(newModifier))
      .build
  }

  def toProductPricesList(productPrice: ProductPrice): List[ProductPrices] = {
    List(ProductPrices.newBuilder.addAllProductPrices(Iterable(productPrice).asJava).build)
  }
}
