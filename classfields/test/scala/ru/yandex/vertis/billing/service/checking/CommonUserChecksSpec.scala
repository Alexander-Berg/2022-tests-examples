package ru.yandex.vertis.billing.service.checking

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.ServiceObject.Kinds
import ru.yandex.vertis.billing.model_core._

/**
  * Tests for [[CommonUserChecks]]
  *
  * @author alesavin
  */
class CommonUserChecksSpec extends AnyWordSpec with Matchers {

  "common checks" should {
    "pass only PartnerOfferId offerIds" in {

      def check: OfferId => Boolean = {
        case _: PartnerOfferId => true
        case _ => false
      }

      CommonUserChecks.offerIdsTypeCheck(Iterable(), check).get
      CommonUserChecks.offerIdsTypeCheck(Iterable(PartnerOfferId("1", "2")), check).get
      CommonUserChecks
        .offerIdsTypeCheck(
          Iterable(
            PartnerOfferId("1", "2"),
            PartnerOfferId("3", "4")
          ),
          check
        )
        .get

      intercept[IllegalArgumentException] {
        CommonUserChecks
          .offerIdsTypeCheck(
            Iterable(
              PartnerOfferId("1", "2"),
              Business("3")
            ),
            check
          )
          .get
      }
      intercept[IllegalArgumentException] {
        CommonUserChecks
          .offerIdsTypeCheck(
            Iterable(
              Business("3")
            ),
            check
          )
          .get
      }
      intercept[IllegalArgumentException] {
        CommonUserChecks
          .offerIdsTypeCheck(
            Iterable(
              ServiceObject(Kinds.NewBuilding, "3", "4")
            ),
            check
          )
          .get
      }
      intercept[IllegalArgumentException] {
        CommonUserChecks
          .offerIdsTypeCheck(
            Iterable(
              ServiceObject(Kinds.Suburban, "3", "4")
            ),
            check
          )
          .get
      }

    }
    "pass only PartnerOfferId and Business offerIds" in {

      def check: OfferId => Boolean = {
        case _: PartnerOfferId => true
        case _: Business => true
        case _ => false
      }

      CommonUserChecks.offerIdsTypeCheck(Iterable(), check).get
      CommonUserChecks.offerIdsTypeCheck(Iterable(PartnerOfferId("1", "2")), check).get
      CommonUserChecks
        .offerIdsTypeCheck(
          Iterable(
            PartnerOfferId("1", "2"),
            PartnerOfferId("3", "4")
          ),
          check
        )
        .get

      CommonUserChecks
        .offerIdsTypeCheck(
          Iterable(
            PartnerOfferId("1", "2"),
            Business("3")
          ),
          check
        )
        .get
      CommonUserChecks
        .offerIdsTypeCheck(
          Iterable(
            Business("3")
          ),
          check
        )
        .get

      intercept[IllegalArgumentException] {
        CommonUserChecks
          .offerIdsTypeCheck(
            Iterable(
              ServiceObject(Kinds.NewBuilding, "3", "4")
            ),
            check
          )
          .get
      }
    }
  }
}
