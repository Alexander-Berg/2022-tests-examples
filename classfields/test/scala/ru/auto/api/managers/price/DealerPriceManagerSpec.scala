package ru.auto.api.managers.price

import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatest.{Inspectors, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.exceptions.NeedAuthentication
import ru.auto.api.extdata.DataService
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.price.DealerPriceManager.toApiPrice
import ru.auto.api.model.AutoruProduct._
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils.RichOfferOrBuilder
import ru.auto.api.model.bunker.DealerVasDescriptionBase
import ru.auto.api.services.billing.MoishaClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.{geo, BaseSpec}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class DealerPriceManagerSpec
  extends BaseSpec
  with ScalaCheckPropertyChecks
  with MockitoSupport
  with TestRequest
  with OptionValues
  // without this non-empty list gens may generate weird Nil values after test failures
  with ShrinkLowPriority {

  private val moishaClient = mock[MoishaClient]
  private val vosClient = mock[VosClient]
  private val geoTree = mock[geo.Tree]
  private val vasDescriptions = mock[DealerVasDescriptionBase]
  private val testData = mock[DataService]
  when(testData.dealerVasDescriptionBase).thenReturn(vasDescriptions)
  when(testData.tree).thenReturn(geoTree)

  private val manager = new DealerPriceManager(moishaClient, vosClient, testData)

  private val productPriceRur = 300
  private val productPriceCents = 30000L

  private def verifyAllNoMoreInteractions(): Unit = {
    verifyNoMoreInteractions(moishaClient, vosClient, geoTree, vasDescriptions)
  }

  private def resetAll(): Unit = {
    reset[AnyRef](moishaClient, vosClient, geoTree, vasDescriptions)
  }

  private def mockEmptyVasDescriptions(): Unit = {
    // scalac fails without type ascription:
    // Error: overloaded method value thenReturn with alternatives:
    // (x$1: Map[String,ru.auto.api.managers.price.VasDescription],x$2: Map[String,ru.auto.api.managers.price.VasDescription]*)org.mockito.stubbing.OngoingStubbing[Map[String,ru.auto.api.managers.price.VasDescription]] <and>
    // (x$1: Map[String,ru.auto.api.managers.price.VasDescription])org.mockito.stubbing.OngoingStubbing[Map[String,ru.auto.api.managers.price.VasDescription]]
    // cannot be applied to (scala.collection.immutable.Map[Nothing,Nothing])
    when(vasDescriptions.descriptions).thenReturn(Map[String, VasDescription]())
  }

  "DealerPriceManager.getDealerPrices()" should {

    "get price" in {
      forAll(
        // forAll is defined only up to six args
        // we zip them all into one arg as workaround
        Gen.zip(
          DealerOfferGen,
          AutoruVasProductGen,
          moishaPointGen(),
          vasDescriptionGen(),
          Gen.posNum[Long],
          RegionGen,
          RegionGen
        )
      ) {
        case (offer, product, price, description, dealerRegionId, region, city) =>
          resetAll()
          val mappedDescription = Map(product.salesName -> description)
          when(vasDescriptions.descriptions).thenReturn(mappedDescription)
          val expected = Seq(manager.enrichPrice(toApiPrice(price, product)))
          when(vosClient.getDealerRegionId(?)(?)).thenReturnF(dealerRegionId)
          when(geoTree.unsafeFederalSubject(?)).thenReturn(region)
          when(geoTree.city(any[Long]())).thenReturn(Some(city))
          when(moishaClient.getPrice(?, ?, ?, ?, ?)(?, ?)).thenReturnF(price)
          manager.getDealerPrices(offer, Seq(product)).futureValue shouldBe expected
          verify(vosClient).getDealerRegionId(eq(offer.userRef.asDealer))(?)
          verify(geoTree).unsafeFederalSubject(dealerRegionId)
          verify(geoTree).city(dealerRegionId)
          // Для обычных услуг vasDescriptions.descriptions вызывается по одному разу на каждую услугу.
          // При этом мы обогащаем услугу в тесте дважды: при вызове
          // manager.enrichPrice(toApiPrice(...)) и при вызове manager.getDealerPrices(...).
          // Поэтому обращение к списку описаний услуг будет выполнено как минимум дважды.
          // Для пакетов обращение идёт не только для самого пакета, но и для каждой услуги из пакета.
          // В таком случае обращений к списку описаний будет больше, поэтому нужен atLeast().
          // Нельзя импортировать atLeast() из-за конфликта имён с org.scalatest.Matchers#atLeast.
          verify(vasDescriptions, Mockito.atLeast(2)).descriptions
          verify(moishaClient).getPrice(eq(offer), eq(region.id), eq(Some(city.id)), eq(product), eq(None))(?, ?)
          verifyAllNoMoreInteractions()
      }
    }

    "get price without descriptions" in {
      forAll(DealerOfferGen, AutoruVasProductGen, moishaPointGen(), Gen.posNum[Long], RegionGen, RegionGen) {
        (offer, product, price, dealerRegionId, region, city) =>
          resetAll()
          mockEmptyVasDescriptions()
          val expected = Seq(manager.enrichPrice(toApiPrice(price, product)))
          when(vosClient.getDealerRegionId(?)(?)).thenReturnF(dealerRegionId)
          when(geoTree.unsafeFederalSubject(?)).thenReturn(region)
          when(geoTree.city(any[Long]())).thenReturn(Some(city))
          when(moishaClient.getPrice(?, ?, ?, ?, ?)(?, ?)).thenReturnF(price)
          manager.getDealerPrices(offer, Seq(product)).futureValue shouldBe expected
          verify(vosClient).getDealerRegionId(eq(offer.userRef.asDealer))(?)
          verify(geoTree).unsafeFederalSubject(dealerRegionId)
          verify(geoTree).city(dealerRegionId)
          verify(vasDescriptions, Mockito.atLeast(1)).descriptions
          verify(moishaClient).getPrice(eq(offer), eq(region.id), eq(Some(city.id)), eq(product), eq(None))(?, ?)
          verifyAllNoMoreInteractions()
      }
    }

    "get two prices" in {
      forAll(
        DealerOfferGen,
        Gen.listOfN(2, AutoruVasProductGen).filter {
          case List(p1, p2) => p1 != p2
          case _ => false
        },
        Gen.listOfN(2, moishaPointGen(productPriceCents)),
        Gen.listOfN(2, vasDescriptionGen()),
        Gen.posNum[Long],
        RegionGen
      ) { (offer, products, prices, descriptions, dealerRegionId, region) =>
        resetAll()
        val List(product1, product2) = products: @unchecked
        val List(price1, price2) = prices: @unchecked
        val List(description1, description2) = descriptions: @unchecked
        val mappedDescriptions = Map(product1.salesName -> description1, product2.salesName -> description2)
        when(vasDescriptions.descriptions).thenReturn(mappedDescriptions)
        val expected = Seq(
          manager.enrichPrice(toApiPrice(price1, product1)),
          manager.enrichPrice(toApiPrice(price2, product2))
        )
        when(vosClient.getDealerRegionId(?)(?)).thenReturnF(dealerRegionId)
        when(geoTree.unsafeFederalSubject(?)).thenReturn(region)
        when(geoTree.city(any[Long]())).thenReturn(None)
        when(moishaClient.getPrice(?, ?, ?, eq(product1), eq(None))(?, ?)).thenReturnF(price1)
        when(moishaClient.getPrice(?, ?, ?, eq(product2), eq(None))(?, ?)).thenReturnF(price2)
        manager.getDealerPrices(offer, products).futureValue shouldBe expected
        verify(vosClient).getDealerRegionId(eq(offer.userRef.asDealer))(?)
        verify(geoTree).unsafeFederalSubject(dealerRegionId)
        verify(geoTree).city(dealerRegionId)
        verify(vasDescriptions, Mockito.atLeast(4)).descriptions
        verify(moishaClient).getPrice(eq(offer), eq(region.id), eq(None), eq(product1), eq(None))(?, ?)
        verify(moishaClient).getPrice(eq(offer), eq(region.id), eq(None), eq(product2), eq(None))(?, ?)
        verifyAllNoMoreInteractions()
      }
    }

    "get price on empty city" in {
      forAll(DealerOfferGen, AutoruVasProductGen, moishaPointGen(), vasDescriptionGen(), Gen.posNum[Long], RegionGen) {
        (offer, product, price, description, dealerRegionId, region) =>
          resetAll()
          val mappedDescription = Map(product.salesName -> description)
          when(vasDescriptions.descriptions).thenReturn(mappedDescription)
          val expected = Seq(manager.enrichPrice(toApiPrice(price, product)))
          when(vosClient.getDealerRegionId(?)(?)).thenReturnF(dealerRegionId)
          when(geoTree.unsafeFederalSubject(?)).thenReturn(region)
          when(geoTree.city(any[Long]())).thenReturn(None)
          when(moishaClient.getPrice(?, ?, ?, ?, ?)(?, ?)).thenReturnF(price)
          manager.getDealerPrices(offer, Seq(product)).futureValue shouldBe expected
          verify(vosClient).getDealerRegionId(eq(offer.userRef.asDealer))(?)
          verify(geoTree).unsafeFederalSubject(dealerRegionId)
          verify(geoTree).city(dealerRegionId)
          verify(vasDescriptions, Mockito.atLeast(2)).descriptions
          verify(moishaClient).getPrice(eq(offer), eq(region.id), eq(None), eq(product), eq(None))(?, ?)
          verifyAllNoMoreInteractions()
      }
    }

    "not get private offer prices" in {
      forAll(PrivateOfferGen, Gen.nonEmptyListOf(ProductGen)) { (offer, products) =>
        resetAll()
        val result = manager.getDealerPrices(offer, products).failed.futureValue
        result shouldBe a[NeedAuthentication]
        verifyAllNoMoreInteractions()
      }
    }

    "not get prices for empty products" in {
      forAll(DealerOfferGen) { offer =>
        resetAll()
        manager.getDealerPrices(offer, Seq()).failed.futureValue shouldBe an[Exception]
        verifyAllNoMoreInteractions()
      }
    }
  }

  "DealerPriceManager.enrichPrice(toApiPrice())" should {

    "return turbo-package price with days = 7" in {
      mockEmptyVasDescriptions()
      forAll(moishaPointGen()) { price =>
        val res = manager.enrichPrice(toApiPrice(price, PackageTurbo))
        res.getDays shouldBe 7
      }
    }

    "return placement price with days = 1" in {
      mockEmptyVasDescriptions()
      forAll(moishaPointGen()) { price =>
        val res = manager.enrichPrice(toApiPrice(price, Placement))
        res.getDays shouldBe 1
      }
    }

    "return boost price with days = 1" in {
      mockEmptyVasDescriptions()
      forAll(moishaPointGen()) { price =>
        val res = manager.enrichPrice(toApiPrice(price, Boost))
        res.getDays shouldBe 1
      }
    }

    "not set package_services field for non-package product" in {
      mockEmptyVasDescriptions()
      forAll(moishaPointGen()) { price =>
        val res = manager.enrichPrice(toApiPrice(price, Placement))
        res.getPackageServicesList.asScala shouldBe empty
      }
    }

    "add two package_services for turbo package" in {
      mockEmptyVasDescriptions()
      forAll(moishaPointGen()) { price =>
        val res = manager.enrichPrice(toApiPrice(price, PackageTurbo))
        val packageServices = res.getPackageServicesList.asScala
        packageServices should have size 2
      }
    }

    "set premium in package_services field for turbo package" in {
      mockEmptyVasDescriptions()
      forAll(moishaPointGen()) { price =>
        val res = manager.enrichPrice(toApiPrice(price, PackageTurbo))
        val packageServices = res.getPackageServicesList.asScala
        packageServices
          .find(_.getService == "all_sale_premium")
          .getOrElse(fail("Turbo package must contain premium in package_services field"))
      }
    }

    "set special in package_services field for turbo package" in {
      mockEmptyVasDescriptions()
      forAll(moishaPointGen()) { price =>
        val res = manager.enrichPrice(toApiPrice(price, PackageTurbo))
        val packageServices = res.getPackageServicesList.asScala
        packageServices
          .find(_.getService == "all_sale_special")
          .getOrElse(fail("Turbo package must contain special in package_services field"))
      }
    }

    "set days = 7 in package_services field for turbo package" in {
      mockEmptyVasDescriptions()
      forAll(moishaPointGen()) { price =>
        val packageServices = manager.enrichPrice(toApiPrice(price, PackageTurbo)).getPackageServicesList.asScala
        packageServices should not be empty
        Inspectors.forEvery(packageServices)(service => service.getDays shouldBe 7)
      }
    }

    "set empty prices in package_services field for turbo package" in {
      mockEmptyVasDescriptions()
      forAll(moishaPointGen()) { price =>
        val packageServices = manager.enrichPrice(toApiPrice(price, PackageTurbo)).getPackageServicesList.asScala
        packageServices should not be empty
        Inspectors.forEvery(packageServices) { service =>
          service.getPrice shouldBe 0
          service.hasAutoProlongPrice shouldBe false
          service.getOriginalPrice shouldBe 0
          service.getAutoApplyPrice shouldBe 0
        }
      }
    }

    "set premium in package_services field for turbo package with descriptions" in {
      forAll(moishaPointGen(), vasDescriptionGen()) { (price, description) =>
        resetAll()
        when(vasDescriptions.descriptions).thenReturn(Map("all_sale_premium" -> description))
        val res = manager.enrichPrice(toApiPrice(price, PackageTurbo))
        val packageServices = res.getPackageServicesList.asScala
        val premium = packageServices
          .find(_.getService == "all_sale_premium")
          .value
        premium.getName shouldBe description.name
        premium.getTitle shouldBe description.title
        premium.getDescription shouldBe description.description
        premium.getDescriptionApp shouldBe description.descriptionApp.getOrElse(description.description)
      }
    }

    "set special in package_services field for turbo package with descriptions" in {
      forAll(moishaPointGen(), vasDescriptionGen()) { (price, description) =>
        resetAll()
        when(vasDescriptions.descriptions).thenReturn(Map("all_sale_special" -> description))
        val res = manager.enrichPrice(toApiPrice(price, PackageTurbo))
        val packageServices = res.getPackageServicesList.asScala
        val premium = packageServices
          .find(_.getService == "all_sale_special")
          .value
        premium.getName shouldBe description.name
        premium.getTitle shouldBe description.title
        premium.getDescription shouldBe description.description
        premium.getDescriptionApp shouldBe description.descriptionApp.getOrElse(description.description)
      }
    }
  }

  "DealerPriceManager.toApiPrice()" should {

    "calculate correctly" in {
      forAll(moishaPointGen(productPriceCents)) { price =>
        val result = toApiPrice(price, Placement)
        result.getService shouldBe "all_sale_activate"
        result.getPrice shouldBe productPriceRur
        result.getCurrency shouldBe "RUR"
        result.getNeedConfirm shouldBe true
        result.getRecommendationPriority shouldBe 0
      }
    }

    "calculate recommendation priority correctly" in {
      forAll(moishaPointGen(productPriceCents), ProductGen) { (price, product) =>
        val res = toApiPrice(price, product)
        val priority = res.getRecommendationPriority
        product match {
          case Reset => priority shouldBe 6
          case Boost => priority shouldBe 5
          case PackageTurbo => priority shouldBe 4
          case Premium => priority shouldBe 3
          case SpecialOffer => priority shouldBe 2
          case Badge => priority shouldBe 1
          case _ => priority shouldBe 0
        }
      }
    }

  }
}
