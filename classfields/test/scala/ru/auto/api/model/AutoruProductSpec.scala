package ru.auto.api.model

import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.BaseSpec
import ru.auto.api.features.FeatureManager.DealerVasProductsFeatures
import ru.auto.api.model.AutoruProduct._
import ru.auto.api.model.salesman.CallPaymentAvailability
import ru.yandex.vertis.feature.model.Feature

class AutoruProductSpec extends BaseSpec {

  private def showInStories(enable: Boolean) = new Feature[Boolean] {
    override def name: String = ""
    override def value: Boolean = enable
  }

  private def dealerVasProductsFeatures(dealerRegionId: Long) = DealerVasProductsFeatures(
    Seq(),
    Feature("", _ => true),
    Feature("", _ => List(dealerRegionId)),
    Feature("", _ => List(dealerRegionId)),
    Feature("", _ => List(dealerRegionId))
  )

  val otherProducts = Set(
    Premium,
    CertificationPlanned,
    CertificationMobile,
    SaleAdd,
    PackageCart,
    StoTop,
    OffersHistoryReports,
    VinHistory,
    ConciergePrepay,
    TradeInRequestCarsNew,
    TradeInRequestCarsUsed,
    MatchApplicationCarsNew,
    Call,
    Booking,
    Reset
  )

  "Products".can {
    "if ShowInStories enabled" should {
      "contain all user products in autoruUserProducts" in {
        autoruUserProducts(showInStories(enable = true)).toSet shouldBe
          AutoruProduct.values.diff(otherProducts)
      }
    }

    "if ShowInStories disabled" should {
      "contain all user products in autoruUserProducts" in {
        autoruUserProducts(showInStories(enable = false)).toSet shouldBe (
          AutoruProduct.values.diff(otherProducts) - ShowInStories
        )
      }
    }

    "AutoruProduct.getDealerVasProducts" should {

      "not filter Premium and Boost for cars new if feature enabled and contains dealer region" in {

        val dealerRegionId = 1L

        val productsWithRegionMatch = AutoruProduct.getDealerVasProducts(
          Category.CARS,
          Section.NEW,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )
        productsWithRegionMatch.contains(AutoruProduct.Premium) shouldBe true
        productsWithRegionMatch.contains(AutoruProduct.Boost) shouldBe true
      }

      "not filter Premium and Boost for trucks if feature enabled and contains dealer region" in {

        val dealerRegionId = 1L

        val trucksNewProducts = AutoruProduct.getDealerVasProducts(
          Category.TRUCKS,
          Section.NEW,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )
        trucksNewProducts.contains(AutoruProduct.Premium) shouldBe true
        trucksNewProducts.contains(AutoruProduct.Boost) shouldBe true

        val trucksUsedProducts = AutoruProduct.getDealerVasProducts(
          Category.TRUCKS,
          Section.NEW,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )
        trucksUsedProducts.contains(AutoruProduct.Premium) shouldBe true
        trucksUsedProducts.contains(AutoruProduct.Boost) shouldBe true
      }

      "not filter Premium and Boost for moto if feature enabled and contains dealer region" in {

        val dealerRegionId = 1L

        val motoNewProducts = AutoruProduct.getDealerVasProducts(
          Category.MOTO,
          Section.NEW,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )
        motoNewProducts.contains(AutoruProduct.Premium) shouldBe true
        motoNewProducts.contains(AutoruProduct.Boost) shouldBe true

        val motoUsedProducts = AutoruProduct.getDealerVasProducts(
          Category.MOTO,
          Section.NEW,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )
        motoUsedProducts.contains(AutoruProduct.Premium) shouldBe true
        motoUsedProducts.contains(AutoruProduct.Boost) shouldBe true
      }

      "filter Turbo for cars used if feature enabled and contains dealer region" in {

        val dealerRegionId = 1L

        val productsWithoutRegionMatch = AutoruProduct.getDealerVasProducts(
          Category.CARS,
          Section.USED,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId + 10L)
        )

        productsWithoutRegionMatch.contains(AutoruProduct.PackageTurbo) shouldBe true

        val productsWithRegionMatch = AutoruProduct.getDealerVasProducts(
          Category.CARS,
          Section.USED,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )
        productsWithRegionMatch.contains(AutoruProduct.PackageTurbo) shouldBe false
      }

      "filter Premium and Boost for cars used if feature enabled and contains dealer region" in {

        val dealerRegionId = 1L

        val productsWithoutRegionMatch = AutoruProduct.getDealerVasProducts(
          Category.CARS,
          Section.USED,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId + 10L)
        )
        productsWithoutRegionMatch.contains(AutoruProduct.Premium) shouldBe true
        productsWithoutRegionMatch.contains(AutoruProduct.Boost) shouldBe true

        val productsWithRegionMatch = AutoruProduct.getDealerVasProducts(
          Category.CARS,
          Section.USED,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )

        productsWithRegionMatch.contains(AutoruProduct.Premium) shouldBe false
        productsWithRegionMatch.contains(AutoruProduct.Boost) shouldBe false
      }

      "not filter Reset if feature enabled and contains dealer region for CARS USED" in {

        val dealerRegionId = 1L

        val productsWithRegionMatch = AutoruProduct.getDealerVasProducts(
          Category.CARS,
          Section.USED,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )
        productsWithRegionMatch.contains(AutoruProduct.Reset) shouldBe true
      }

      "filter Reset if feature enabled and contains dealer region for CARS NEW" in {

        val dealerRegionId = 1L

        val productsWithRegionMatch = AutoruProduct.getDealerVasProducts(
          Category.CARS,
          Section.NEW,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )
        productsWithRegionMatch.contains(AutoruProduct.Reset) shouldBe false
      }

      "filter Reset if feature enabled and contains dealer region for MOTO" in {

        val dealerRegionId = 1L

        val productsWithRegionMatch = AutoruProduct.getDealerVasProducts(
          Category.MOTO,
          Section.NEW,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(dealerRegionId)
        )
        productsWithRegionMatch.contains(AutoruProduct.Reset) shouldBe false
      }

      "filter Reset if feature not contains dealer region" in {

        val dealerRegionId = 1L
        val featureRegionId = 2L

        val productsWithRegionMatch = AutoruProduct.getDealerVasProducts(
          Category.CARS,
          Section.USED,
          CallPaymentAvailability.NoCalls,
          dealerRegionId,
          dealerVasProductsFeatures(featureRegionId)
        )
        productsWithRegionMatch.contains(AutoruProduct.Reset) shouldBe false
      }
    }

  }
}
