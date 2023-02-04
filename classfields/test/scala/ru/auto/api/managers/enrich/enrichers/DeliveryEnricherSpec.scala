package ru.auto.api.managers.enrich.enrichers

import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel._
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.RegionInfo
import ru.auto.api.auth.Application
import ru.auto.api.geo.{Region, RegionTypes, Tree}
import ru.auto.api.managers.delivery.DeliveryRegionProductPriceManager
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.managers.enrich.enrichers.delivery.DeliveryEnricher
import ru.auto.api.model.ModelGenerators.DealerOfferGen
import ru.auto.api.model.{AutoruDealer, AutoruProduct, RequestParams, Version}
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.mockito.MockitoSupport
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.salesman.{Campaign, PaymentModel}
import ru.auto.api.services.salesman.SalesmanClient

import scala.jdk.CollectionConverters._
import scala.util.Success

class DeliveryEnricherSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  private val productPriceManager = mock[DeliveryRegionProductPriceManager]
  private val geoTree = mock[Tree]
  private val salesmanClient = mock[SalesmanClient]
  private val deliveryEnricher = new DeliveryEnricher(productPriceManager, geoTree, salesmanClient)

  "DeliveryEnricher" should {

    "enrich with prices and location if both options present" in {
      forAll(DealerOfferGen) { offer =>
        reset(geoTree, productPriceManager)
        val products = getProductsPricesInRegion(offer)
        val expectedRegionInfo = prepareExpectedRegionInfoViaMocking
        when(productPriceManager.getActiveProductsPriceInRegion(?, ?, ?, ?)(?))
          .thenReturnF(
            ProductsPricesInRegions
              .newBuilder()
              .addAllProductsPricesInRegions(products.asJava)
              .build()
          )
        when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(
          Set(
            Campaign(
              paymentModel = PaymentModel.Single,
              tag = "",
              category = offer.getCategory.toString.toLowerCase,
              subcategory = Nil,
              section = List(offer.getSection.toString.toLowerCase),
              size = 0,
              enabled = true
            )
          )
        )

        val enrich =
          deliveryEnricher
            .getFunction(
              Seq(offer),
              EnrichOptions(deliveryRegionsActiveProductPrices = true, deliveryRegionsLocationRegionInfo = true)
            )(createRequest(offer.dealerUserRef))
            .futureValue

        val enrichedOffer = enrich(offer)

        val deliveryInfo: DeliveryInfo = enrichedOffer.getDeliveryInfo
        val deliveryRegions: List[DeliveryRegion] = deliveryInfo.getDeliveryRegionsList.asScala.toList

        deliveryRegions.foreach { deliveryRegion =>
          deliveryRegion.getProductsList.asScala.toList shouldBe products
          val regionInfo = deliveryRegion.getLocation.getRegionInfo
          regionInfo shouldBe expectedRegionInfo
        }
      }
    }

    "enrich only with prices if products prices enrich option present" in {
      forAll(DealerOfferGen) { offer =>
        reset(geoTree, productPriceManager)
        val products = {
          val products = offer.getServicesList.asScala.map(service =>
            ProductPriceInRegion
              .newBuilder()
              .setPrice(100500)
              .setProduct(service.getService)
              .build()
          )
          if (offer.getBadgesList.isEmpty) {
            products
          } else {
            products :+ ProductPriceInRegion
              .newBuilder()
              .setPrice(100500)
              .setProduct(AutoruProduct.Badge.toString)
              .build()
          }
        }

        when(productPriceManager.getActiveProductsPriceInRegion(?, ?, ?, ?)(?))
          .thenReturnF(
            ProductsPricesInRegions
              .newBuilder()
              .addAllProductsPricesInRegions(products.asJava)
              .build()
          )
        when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(
          Set(
            Campaign(
              paymentModel = PaymentModel.Single,
              tag = "",
              category = offer.getCategory.toString.toLowerCase,
              subcategory = Nil,
              section = List(offer.getSection.toString.toLowerCase),
              size = 0,
              enabled = true
            )
          )
        )

        val enrich =
          deliveryEnricher
            .getFunction(Seq(offer), EnrichOptions(deliveryRegionsActiveProductPrices = true))(
              createRequest(offer.dealerUserRef)
            )
            .futureValue
        val enrichedOffer = enrich(offer)

        val deliveryInfo: DeliveryInfo = enrichedOffer.getDeliveryInfo
        val deliveryRegions: List[DeliveryRegion] = deliveryInfo.getDeliveryRegionsList.asScala.toList

        deliveryRegions.foreach(deliveryRegion => deliveryRegion.getProductsList.asScala.toList shouldBe products)
        verifyNoMoreInteractions(geoTree)
      }
    }

    "enrich only with RegionInfo if regions enrich option is present" in {
      forAll(DealerOfferGen) { offer =>
        reset(geoTree, productPriceManager)
        when(salesmanClient.getCampaigns(?, ?)(?)).thenReturnF(
          Set(
            Campaign(
              paymentModel = PaymentModel.Single,
              tag = "",
              category = offer.getCategory.toString.toLowerCase,
              subcategory = Nil,
              section = List(offer.getSection.toString.toLowerCase),
              size = 0,
              enabled = true
            )
          )
        )

        val expectedRegionInfo = prepareExpectedRegionInfoViaMocking

        val enrichFunc = deliveryEnricher
          .getFunction(Seq(offer), EnrichOptions(deliveryRegionsLocationRegionInfo = true))(
            createRequest(offer.dealerUserRef)
          )
          .futureValue
        val enrichedOffer = enrichFunc(offer)
        val deliveryRegions =
          enrichedOffer.getDeliveryInfo.getDeliveryRegionsList.asScala.toList

        deliveryRegions.foreach { region =>
          val regionInfo = region.getLocation.getRegionInfo
          regionInfo shouldBe expectedRegionInfo
        }
        verifyNoMoreInteractions(productPriceManager)
      }
    }

    "not enrich if no options present" in {
      forAll(DealerOfferGen) { offer =>
        reset(geoTree, productPriceManager)
        val enrich =
          deliveryEnricher.getFunction(Seq(offer), EnrichOptions())(createRequest(offer.dealerUserRef)).futureValue
        enrich(offer)
        verifyNoMoreInteractions(geoTree)
        verifyNoMoreInteractions(productPriceManager)
      }
    }
  }

  private def prepareExpectedRegionInfoViaMocking: RegionInfo = {
    val federalSubjectId = 3L
    val cityId = 2L
    val cityRegion = Region(
      cityId,
      3L,
      RegionTypes.City,
      "Msk",
      "Msk",
      "Msk",
      "Msk",
      "Msk",
      "Msk",
      1.0,
      2.0,
      1,
      None
    )
    val federalSubject = cityRegion.copy(id = federalSubjectId)
    when(geoTree.region(cityId))
      .thenReturn(Some(cityRegion))
    when(geoTree.findCityByRegionId(any[Long]()))
      .thenReturn(Success(cityRegion))
    when(geoTree.unsafeFederalSubject(any[Long]()))
      .thenReturn(federalSubject)
    when(geoTree.pathToRoot(cityRegion))
      .thenReturn(Nil)

    RegionInfo
      .newBuilder()
      .setId(cityId)
      .setName("Msk")
      .setGenitive("Msk")
      .setDative("Msk")
      .setAccusative("Msk")
      .setPrepositional("Msk")
      .setPreposition("Msk")
      .setLongitude(cityRegion.longitude)
      .setLatitude(cityRegion.latitude)
      .build()
  }

  private def createRequest(userRef: AutoruDealer) = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setVersion(Version.V1_0)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(userRef)
    r.setDealer(userRef)
    r.named("test")
    r
  }

  private def getProductsPricesInRegion(offer: Offer) = {
    val products = offer.getServicesList.asScala.map(service =>
      ProductPriceInRegion
        .newBuilder()
        .setPrice(100500)
        .setProduct(service.getService)
        .build()
    )
    if (offer.getBadgesList.isEmpty) {
      products.toList
    } else {
      (products :+ ProductPriceInRegion
        .newBuilder()
        .setPrice(100500)
        .setProduct(AutoruProduct.Badge.toString)
        .build()).toList
    }
  }
}
