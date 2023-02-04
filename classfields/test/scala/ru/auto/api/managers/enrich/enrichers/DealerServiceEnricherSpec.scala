package ru.auto.api.managers.enrich.enrichers

import org.scalatest.{EitherValues, Inspectors, OptionValues}
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.PaidService
import ru.auto.api.CommonModel.PaidService.Activator.{OWNER, PACKAGE_TURBO}
import DealerServiceEnricher.enrichService

class DealerServiceEnricherSpec extends BaseSpec with OptionValues with EitherValues {

  private val enricher = new DealerServiceEnricher

  "Dealer service enricher" should {

    "enrich non-package service and not add more services" in {
      val service = PaidService.newBuilder().setService("all_sale_special").build()
      val result = enrichService(service)
      result should have size 1
    }

    "enrich non-package service and keep base fields" in {
      val service = PaidService
        .newBuilder()
        .setService("all_sale_special")
        .setIsActive(true)
        .setCreateDate(1234)
        .setExpireDate(4321)
        .build()
      val result = enrichService(service).headOption.value
      result.getService shouldBe "all_sale_special"
      result.getIsActive shouldBe true
      result.getCreateDate shouldBe 1234
      result.getExpireDate shouldBe 4321
    }

    "enrich badge service and keep base fields" in {
      val service = PaidService
        .newBuilder()
        .setService("all_sale_badge")
        .setIsActive(true)
        .setCreateDate(1234)
        .setExpireDate(4321)
        .setBadge("best conditioner")
        .build()
      val result = enrichService(service).headOption.value
      result.getService shouldBe "all_sale_badge"
      result.getIsActive shouldBe true
      result.getCreateDate shouldBe 1234
      result.getExpireDate shouldBe 4321
      result.getBadge shouldBe "best conditioner"
    }

    "enrich prolongable service with prolongable = true" in {
      val service = PaidService.newBuilder().setService("all_sale_special").build()
      val result = enrichService(service).headOption.value
      result.getProlongable shouldBe true
    }

    "enrich service which can be deactivated with deactivation_allowed = true" in {
      val service = PaidService.newBuilder().setService("all_sale_special").build()
      val result = enrichService(service).headOption.value
      result.getDeactivationAllowed.getValue shouldBe true
    }

    "enrich service activated by owner with activation_reason = ACTIVATED_BY_OWNER" in {
      val service = PaidService.newBuilder().setService("all_sale_special").build()
      val result = enrichService(service).headOption.value
      result.getActivatedBy shouldBe OWNER
    }

    "enrich boost and keep base fields" in {
      val service = PaidService
        .newBuilder()
        .setService("all_sale_fresh")
        .setIsActive(true)
        .setCreateDate(1234)
        .setExpireDate(4321)
        .build()
      val result = enrichService(service).headOption.value
      result.getService shouldBe "all_sale_fresh"
      result.getIsActive shouldBe true
      result.getCreateDate shouldBe 1234
      result.getExpireDate shouldBe 4321
    }

    "enrich boost with prolongable = false" in {
      val service = PaidService.newBuilder().setService("all_sale_fresh").build()
      val result = enrichService(service).headOption.value
      result.getProlongable shouldBe false
    }

    "enrich boost with deactivation_allowed = false" in {
      val service = PaidService.newBuilder().setService("all_sale_fresh").build()
      val result = enrichService(service).headOption.value
      result.hasDeactivationAllowed shouldBe true
      result.getDeactivationAllowed.getValue shouldBe false
    }

    "enrich boost with activation_reason = ACTIVATED_BY_OWNER" in {
      val service = PaidService.newBuilder().setService("all_sale_fresh").build()
      val result = enrichService(service).headOption.value
      result.getActivatedBy shouldBe OWNER
    }

    "enrich reset with right fields" in {
      val service = PaidService
        .newBuilder()
        .setService("reset")
        .build()
      val result = enrichService(service).headOption.value
      result.getService shouldBe "reset"
      result.getProlongable shouldBe false
      result.hasDeactivationAllowed shouldBe true
      result.getDeactivationAllowed.getValue shouldBe false
      result.getActivatedBy shouldBe OWNER
    }

    "enrich turbo with two subpackages" in {
      val service = PaidService.newBuilder().setService("package_turbo").build()
      val results = enrichService(service)
      results should have size 3
    }

    "enrich turbo with subpackages: premium & special" in {
      val service = PaidService.newBuilder().setService("package_turbo").build()
      val results = enrichService(service)
      results.map(_.getService) should contain theSameElementsAs List(
        "package_turbo",
        "all_sale_special",
        "all_sale_premium"
      )
    }

    "enrich turbo and subpackages and keep base fields" in {
      val service = PaidService
        .newBuilder()
        .setService("package_turbo")
        .setIsActive(true)
        .setCreateDate(1234)
        .setExpireDate(4321)
        .build()
      val results = enrichService(service)
      Inspectors.forEvery(results) { result =>
        result.getIsActive shouldBe true
        result.getCreateDate shouldBe 1234
        result.getExpireDate shouldBe 4321
      }
    }

    "enrich turbo and subpackages with prolongable = false" in {
      val service = PaidService.newBuilder().setService("package_turbo").build()
      val results = enrichService(service)
      Inspectors.forEvery(results)(result => result.getProlongable shouldBe false)
    }

    "enrich turbo and subpackages with deactivation_allowed = false" in {
      val service = PaidService.newBuilder().setService("package_turbo").build()
      val results = enrichService(service)
      Inspectors.forEvery(results) { result =>
        result.hasDeactivationAllowed shouldBe true
        result.getDeactivationAllowed.getValue shouldBe false
      }
    }

    "enrich turbo with activation_reason = ACTIVATED_BY_OWNER" in {
      val service = PaidService.newBuilder().setService("package_turbo").build()
      val results = enrichService(service)
      val turbo = results.find(_.getService == "package_turbo").value
      turbo.getActivatedBy shouldBe OWNER
    }

    "enrich turbo subpackages with activation_reason = PACKAGE_TURBO" in {
      val service = PaidService.newBuilder().setService("package_turbo").build()
      val results = enrichService(service)
      val subservices = results.filterNot(_.getService == "package_turbo")
      Inspectors.forEvery(subservices)(subservice => subservice.getActivatedBy shouldBe PACKAGE_TURBO)
    }
  }
}
