package ru.auto.api.managers.price

import java.time.OffsetDateTime
import java.util
import java.util.Currency

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.{Offer, ProductPriceInRegion, ProductsPricesInRegions}
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.PaidService
import ru.auto.api.currency.CurrencyRates
import ru.auto.api.exceptions.CustomerAccessForbidden
import ru.auto.api.extdata.DataService
import ru.auto.api.features.FeatureManager
import ru.auto.api.geo.{Region, RegionTypes, Tree}
import ru.auto.api.managers.delivery.DeliveryRegionProductPriceManager
import ru.auto.api.managers.offers.OfferLoader
import ru.auto.api.model.AutoruProduct.{Badge, PackageTurbo, Placement, Premium}
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model._
import ru.auto.api.services.billing.MoishaClient
import ru.auto.api.services.billing.MoishaClient.{MoishaInterval, MoishaPoint, MoishaProduct, ProductDuration}
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.salesman.SalesmanClient.OfferProductActiveDays
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.util.Success

class DeliveryRegionProductPriceManagerSpec
  extends BaseSpec
  with ScalaCheckPropertyChecks
  with MockitoSupport
  with BeforeAndAfter {

  val offerLoader = mock[OfferLoader]
  val moishaClient = mock[MoishaClient]
  val geobaseClient = mock[GeobaseClient]
  val dataService = mock[DataService]
  val tree = mock[Tree]
  val salesmanClient = mock[SalesmanClient]
  val featureManager = mock[FeatureManager]

  val productPriceManager =
    new DeliveryRegionProductPriceManager(
      offerLoader,
      moishaClient,
      geobaseClient,
      dataService,
      salesmanClient,
      featureManager
    )

  private val nearestRegionId = 21L
  private val cityId = 2L
  private val federalSubjectId = 3L
  private val userRef = AutoruDealer(123L)

  val nearestRegion =
    Region(nearestRegionId, cityId, RegionTypes.City, "A", "B", "C", "D", "E", "F", 1.0, 2.0, 1, None)

  val cityRegion =
    Region(cityId, federalSubjectId, RegionTypes.City, "A", "B", "C", "D", "E", "F", 1.0, 2.0, 1, None)

  val federalSubjectRegion =
    Region(federalSubjectId, 0, RegionTypes.City, "A", "B", "C", "D", "E", "F", 1.0, 2.0, 1, None)

  private val offerPlacementDays = Some(28L)

  before {
    reset(moishaClient)
    when(dataService.currency)
      .thenReturn(
        CurrencyRates(
          Map(
            Currency.getInstance("RUR") -> BigDecimal(1),
            Currency.getInstance("USD") -> BigDecimal(65),
            Currency.getInstance("EUR") -> BigDecimal(70)
          )
        )
      )

    when(dataService.tree)
      .thenReturn(tree)
    when(geobaseClient.regionIdByLocation(?, ?)(?))
      .thenReturnF(nearestRegionId)
    when(tree.unsafeFederalSubject(nearestRegionId))
      .thenReturn(federalSubjectRegion)
    when(tree.findCityByRegionId(nearestRegionId))
      .thenReturn(Success(cityRegion))
    when(tree.pathToRoot(cityRegion))
      .thenReturn(Nil)
    when(moishaClient.getPrice(?, ?, ?, ?, ?)(?, ?))
      .thenReturnF(
        MoishaPoint(
          "any",
          MoishaInterval(OffsetDateTime.now(), OffsetDateTime.now()),
          MoishaProduct("", Nil, 20000, Some(ProductDuration.days(1)))
        )
      )
    when(salesmanClient.productActiveDays(?, ?, ?)(?))
      .thenReturnF(OfferProductActiveDays(offerPlacementDays))
  }

  implicit val request: RequestImpl = {
    val req = new RequestImpl
    req.setTrace(Traced.empty)
    req.setRequestParams(RequestParams.construct("1.1.1.1"))
    req.setUser(userRef)
    req
  }

  "ProductPriceManager" should {

    "return price for products in given location" in {
      val badges = util.Arrays.asList("badge0", "badge1")
      val offer = {
        val b = ModelGenerators.OfferGen.next.toBuilder
          .setUserRef(userRef.toPlain)
          .addAllBadges(badges)
        addPlacementServiceIfMissing(b)
        addPremiumAndTurboServicesIfMissing(b)
        b.build()
      }

      when(offerLoader.findRawOffer(?, ?, ?, ?)(?))
        .thenReturnF(offer)

      val expectedResult = buildExpectedResult(offer, cityRegion, badges.size())

      val result = productPriceManager
        .getActiveProductsPriceInRegion(
          OfferID.parse("123-4567"),
          Cars,
          1.0,
          2.0
        )
        .futureValue

      verify(moishaClient)
        .getPrice(
          eq(offer),
          eq(federalSubjectId),
          eq(Some(cityId)),
          eq(AutoruProduct.Badge),
          eq(offerPlacementDays)
        )(?, ?)
      verify(moishaClient)
        .getPrice(
          eq(offer),
          eq(federalSubjectId),
          eq(Some(cityId)),
          eq(AutoruProduct.Placement),
          eq(offerPlacementDays.map(_ + 1))
        )(?, ?)
      verify(
        moishaClient,
        times(
          offer.getServicesList.asScala
            .filterNot(service =>
              service.getService == Premium.salesName
                || service.getService == PackageTurbo.salesName
            )
            .size + 1 /*badge price request*/
        )
      ).getPrice(?, ?, ?, ?, ?)(?, ?)
      result shouldBe expectedResult
    }

    "return price for products in ЫЗИ if unable to find city and parent_id = 10174" in {
      val badges = util.Arrays.asList("badge0", "badge1")
      val offer = {
        val b = ModelGenerators.OfferGen.next.toBuilder
          .setUserRef(userRef.toPlain)
          .addAllBadges(badges)
        addPlacementServiceIfMissing(b)
        addPremiumAndTurboServicesIfMissing(b)
        b.build()
      }
      val nearestRegionWithoutCity = nearestRegion.copy(parentId = 10174)

      when(offerLoader.findRawOffer(?, ?, ?, ?)(?))
        .thenReturnF(offer)
      when(tree.unsafeRegion(nearestRegionId))
        .thenReturn(nearestRegionWithoutCity)
      when(tree.city(nearestRegionWithoutCity))
        .thenReturn(None)
      val spbId = 2
      val spbCityRegion = cityRegion.copy(id = spbId)
      when(tree.unsafeCity(spbId))
        .thenReturn(spbCityRegion)

      val expectedResult = buildExpectedResult(offer, cityRegion, badges.size())

      val result = productPriceManager
        .getActiveProductsPriceInRegion(
          OfferID.parse("123-4567"),
          Cars,
          1.0,
          2.0
        )
        .futureValue

      verify(moishaClient)
        .getPrice(
          eq(offer),
          eq(federalSubjectId),
          eq(Some(spbId)),
          eq(AutoruProduct.Badge),
          eq(offerPlacementDays)
        )(?, ?)
      verify(moishaClient)
        .getPrice(
          eq(offer),
          eq(federalSubjectId),
          eq(Some(cityId)),
          eq(AutoruProduct.Placement),
          eq(offerPlacementDays.map(_ + 1))
        )(?, ?)
      verify(
        moishaClient,
        times(
          offer.getServicesList.asScala
            .filterNot(service =>
              service.getService == Premium.salesName
                || service.getService == PackageTurbo.salesName
            )
            .size + 1 /*badge price request*/
        )
      ).getPrice(?, ?, ?, ?, ?)(?, ?)
      result shouldBe expectedResult
    }

    "fail for not owner" in {
      val offer = {
        val offerOwner = AutoruDealer(777L)

        val b = ModelGenerators.OfferGen.next.toBuilder
          .setUserRef(offerOwner.toPlain)
        addPlacementServiceIfMissing(b)
        b.build()
      }

      when(offerLoader.findRawOffer(?, ?, ?, ?)(?))
        .thenReturnF(offer)

      val result = productPriceManager
        .getActiveProductsPriceInRegion(
          OfferID.parse("123-4567"),
          Cars,
          1.0,
          2.0
        )

      result.failed.futureValue shouldBe a[CustomerAccessForbidden]
    }

    "return price for placement with updated creationDate if offer has no active products" in {
      val offer = {
        val b = ModelGenerators.OfferGen.next.toBuilder
          .setUserRef(userRef.toPlain)
          .clearServices()
        b.build()
      }

      when(offerLoader.findRawOffer(?, ?, ?, ?)(?))
        .thenReturnF(offer)

      val expectedResult = ProductsPricesInRegions
        .newBuilder()
        .setRegionInfo(ModelUtils.asRegionInfoWithParents(tree)(cityRegion))
        .addProductsPricesInRegions(
          ProductPriceInRegion
            .newBuilder()
            .setPrice(200)
            .setRegionId(cityId.toInt)
            .setProduct(Placement.name)
            .setProductOldName(Placement.salesName)
            .build()
        )
        .build()

      val result = productPriceManager
        .getActiveProductsPriceInRegion(
          OfferID.parse("123-4567"),
          Cars,
          1.0,
          2.0
        )
        .futureValue

      verify(moishaClient)
        .getPrice(
          eq(offer),
          eq(federalSubjectId),
          eq(Some(cityId)),
          eq(AutoruProduct.forSalesName(AutoruProduct.Placement.salesName).get),
          eq(offerPlacementDays.map(_ + 1))
        )(?, ?)

      result shouldBe expectedResult
    }
  }

  private def buildExpectedResult(offer: Offer, city: Region, badgesSize: Int) = {
    ProductsPricesInRegions
      .newBuilder()
      .addAllProductsPricesInRegions(
        offer.getServicesList.asScala
          .filterNot(service =>
            service.getService == Premium.salesName
              || service.getService == PackageTurbo.salesName
          )
          .map(service =>
            ProductPriceInRegion
              .newBuilder()
              .setPrice(200)
              .setRegionId(city.id.toInt)
              .setProduct(
                AutoruProduct
                  .forSalesName(service.getService)
                  .getOrElse(throw new Error())
                  .toString
              )
              .setProductOldName(service.getService)
              .build()
          )
          .asJava
      )
      .addAllProductsPricesInRegions(
        util.Arrays.asList(
          ProductPriceInRegion
            .newBuilder()
            .setPrice(200 * badgesSize)
            .setRegionId(city.id.toInt)
            .setProduct(Badge.name)
            .setProductOldName(Badge.salesName)
            .build()
        )
      )
      .setRegionInfo(ModelUtils.asRegionInfoWithParents(tree)(city))
      .build()
  }

  private def addPlacementServiceIfMissing(offer: Offer.Builder): Unit = {
    val services = offer.getServicesList.asScala.toList
    val placementIsMissing = !services
      .map(_.getService)
      .contains(Placement.salesName)

    if (placementIsMissing) {
      offer
        .clearServices()
        .addAllServices(
          (services :+ PaidService
            .newBuilder()
            .setService(Placement.salesName)
            .build()).asJava
        )
    }
  }

  private def addPremiumAndTurboServicesIfMissing(offer: Offer.Builder): Unit = {
    val services = offer.getServicesList.asScala.toList
    val turboIsMissing = !services
      .map(_.getService)
      .contains(PackageTurbo.salesName)
    val premiumIsMissing = !services
      .map(_.getService)
      .contains(Premium.salesName)

    if (turboIsMissing) {
      offer
        .clearServices()
        .addAllServices(
          (services :+ PaidService
            .newBuilder()
            .setService(PackageTurbo.salesName)
            .build()).asJava
        )
    }

    if (premiumIsMissing) {
      offer
        .clearServices()
        .addAllServices(
          (services :+ PaidService
            .newBuilder()
            .setService(Premium.salesName)
            .build()).asJava
        )
    }
  }

}
