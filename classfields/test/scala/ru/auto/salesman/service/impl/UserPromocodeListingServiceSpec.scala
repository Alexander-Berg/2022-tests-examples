package ru.auto.salesman.service.impl

import ru.auto.api.ApiOfferModel
import ru.auto.salesman.Task
import ru.auto.salesman.client.BunkerClient
import ru.auto.salesman.client.PromocoderClient.Services.AutoRuUsers
import ru.auto.salesman.client.impl.AutoRuPromocoderClient
import ru.auto.salesman.environment._
import ru.auto.salesman.model.user.product.ProductProvider
import ru.auto.salesman.model.user.product.Products.OfferProduct
import ru.auto.salesman.model.user.{
  AutoruProductsDescriptions,
  EndUserType,
  ProductDescription,
  ProductsDescriptions
}
import ru.auto.salesman.model.{
  AutoruUser,
  FeatureDiscount,
  FeatureDiscountTypes,
  FeatureInstanceRequest,
  FeaturePayload,
  FeatureTypes,
  FeatureUnits,
  PromocoderUser
}
import ru.auto.salesman.service.impl.user.PromocoderServiceImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.docker.PromocoderContainer
import ru.auto.salesman.util.sttp.SttpClientImpl
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class UserPromocodeListingServiceSpec extends BaseSpec {

  private val topName = "Топ"
  private val boostName = "Буст"
  private val turboName = "Турбо"

  private val names =
    Map[OfferProduct, String](
      ProductProvider.AutoruGoods.Top -> topName,
      ProductProvider.AutoruGoods.Boost -> boostName,
      ProductProvider.AutoruBundles.Turbo -> turboName
    )

  private val promocoderClient = new AutoRuPromocoderClient(
    AutoRuUsers,
    PromocoderContainer.address,
    SttpClientImpl(TestOperationalSupport)
  )

  private val bunkerClient = mock[BunkerClient]

  (bunkerClient.getDescriptions _)
    .expects()
    .returningZ(createDescriptionsMap(names))

  private val promocoderService = new PromocoderServiceImpl(promocoderClient)

  private val productDescriptionService = new ProductDescriptionServiceImpl(
    bunkerClient
  )

  private val service = new UserPromocodeListingService(
    promocoderService,
    productDescriptionService
  )

  private val currentTime = now()
  private val lifetime = 2
  private val deadline = currentTime.plusDays(lifetime).asTimestamp

  private val user = AutoruUser(123)

  private val moneyFeatureRequest = FeatureInstanceRequest(
    tag = "top",
    startTime = Some(currentTime),
    lifetime = lifetime.days,
    count = 1000,
    jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Promocode)
  )

  private val itemFeatureRequest = FeatureInstanceRequest(
    tag = "boost",
    startTime = Some(currentTime),
    lifetime = lifetime.days,
    count = 2,
    jsonPayload = FeaturePayload(FeatureUnits.Items, FeatureTypes.Promocode)
  )

  private val percentFeatureRequest = FeatureInstanceRequest(
    tag = "turbo-package",
    startTime = Some(currentTime),
    lifetime = lifetime.days,
    count = 1,
    jsonPayload = FeaturePayload(
      FeatureUnits.Items,
      FeatureTypes.Promocode,
      discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 70))
    )
  )

  private val loyaltyFeatureRequest = FeatureInstanceRequest(
    tag = "loyalty",
    startTime = Some(currentTime),
    lifetime = 2.days,
    count = 1000,
    jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Loyalty)
  )

  "PromocodeListingServiceImpl" should {

    "discard money promocode" in {
      val result =
        for {
          _ <- ensureListingIsEmpty
          _ <- createFeature(List(moneyFeatureRequest))
          listing <- service.getListing(user)
        } yield listing.getPromocodesList.isEmpty shouldBe true

      result.success.value
    }

    "discard item promocode" in {
      val result =
        for {
          _ <- ensureListingIsEmpty
          _ <- createFeature(List(itemFeatureRequest))
          listing <- service.getListing(user)
        } yield listing.getPromocodesList.isEmpty shouldBe true

      result.success.value
    }

    "return percent promocode" in {
      val result =
        for {
          _ <- ensureListingIsEmpty
          _ <- createFeature(List(percentFeatureRequest))
          listing <- service.getListing(user)
        } yield {
          val promocode = listing.getPromocodesList.asScala.toList.loneElement
          promocode.getDeadline shouldBe deadline
          promocode.getPercent.getCount shouldBe 1
          promocode.getPercent.getPercent shouldBe 70
          promocode.getProduct.getHumanName shouldBe turboName
        }

      result.success.value
    }

    "return several percent promocodes" in {
      val requests = List(
        percentFeatureRequest,
        percentFeatureRequest.copy(tag = "boost")
      )

      val result =
        for {
          _ <- ensureListingIsEmpty
          _ <- createFeature(requests)
          listing <- service.getListing(user)
        } yield listing.getPromocodesList.asScala.toList.size shouldBe 2

      result.success.value
    }

    "return percent promocode and ignore others" in {
      val requests =
        List(
          moneyFeatureRequest,
          itemFeatureRequest,
          percentFeatureRequest,
          loyaltyFeatureRequest
        )

      val result =
        for {
          _ <- ensureListingIsEmpty
          _ <- createFeature(requests)
          listing <- service.getListing(user)
        } yield {
          val promocode = listing.getPromocodesList.asScala.toList.loneElement
          promocode.getDeadline shouldBe deadline
          promocode.getPercent.getCount shouldBe 1
          promocode.getPercent.getPercent shouldBe 70
          promocode.getProduct.getHumanName shouldBe turboName
        }

      result.success.value
    }

  }

  private def ensureListingIsEmpty: Task[Unit] =
    for {
      _ <- promocoderClient.deleteFeatures("123", PromocoderUser(user))
      features <- promocoderService.getFeatures(user)
    } yield
      if (features.isEmpty) ()
      else fail(s"There must be no features at that time")

  private def createFeature(
      requests: List[FeatureInstanceRequest]
  ): Task[Unit] =
    promocoderClient
      .createFeatures("123", PromocoderUser(user), requests)
      .unit

  private def createDescriptionsMap(
      names: Map[OfferProduct, String]
  ): ProductsDescriptions = {
    def toMap(name: String) = {
      val productDescription =
        ProductDescription(name = Some(name))

      val descriptionByType =
        Map[EndUserType, ProductDescription](
          EndUserType.Default -> productDescription
        )

      Map(ApiOfferModel.Category.CARS -> descriptionByType)
    }

    ProductsDescriptions(
      AutoruProductsDescriptions(names.mapValues(toMap), Map.empty)
    )
  }

}
