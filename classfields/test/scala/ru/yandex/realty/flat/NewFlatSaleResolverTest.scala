package ru.yandex.realty.flat

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.model.offer.{CategoryType, DealStatus, FlatType}

@RunWith(classOf[JUnitRunner])
class NewFlatSaleResolverTest extends FlatSpec with Matchers with PropertyChecks {

  val cases =
    Table(
      ("Category", "primarySaleV2", "flatType", "dealStatus", "expected"),
      (CategoryType.APARTMENT, true, FlatType.UNKNOWN, DealStatus.COUNTERSALE, true),
      (CategoryType.APARTMENT, false, FlatType.NEW_FLAT, DealStatus.REASSIGNMENT, true),
      (CategoryType.APARTMENT, false, FlatType.NEW_FLAT, DealStatus.PRIMARY_SALE, true),
      (CategoryType.APARTMENT, false, FlatType.NEW_SECONDARY, DealStatus.REASSIGNMENT, true),
      (CategoryType.APARTMENT, false, FlatType.NEW_SECONDARY, DealStatus.PRIMARY_SALE, true),
      (
        CategoryType.APARTMENT,
        false,
        FlatType.NEW_SECONDARY,
        DealStatus.PRIMARY_SALE_OF_SECONDARY,
        true
      ),
      (CategoryType.APARTMENT, false, FlatType.UNKNOWN, DealStatus.COUNTERSALE, false),
      (CategoryType.APARTMENT, false, FlatType.UNKNOWN, DealStatus.UNKNOWN, false),
      (CategoryType.APARTMENT, false, FlatType.UNKNOWN, DealStatus.COUNTERSALE, false),
      (CategoryType.APARTMENT, false, FlatType.NEW_FLAT, DealStatus.UNKNOWN, false),
      (CategoryType.APARTMENT, false, FlatType.UNKNOWN, DealStatus.PRIMARY_SALE, false),
      (CategoryType.APARTMENT, false, FlatType.UNKNOWN, DealStatus.UNKNOWN, false),
      (CategoryType.APARTMENT, false, FlatType.SECONDARY, DealStatus.PRIMARY_SALE, false),
      (CategoryType.APARTMENT, false, FlatType.NEW_SECONDARY, DealStatus.SALE, false)
    )

  "NewFlatSaleResolver" should "return valid result" in {
    forAll(cases) {
      case (
          category: CategoryType,
          primarySaleV2: Boolean,
          flatType: FlatType,
          dealStatus: DealStatus,
          expected: Boolean
          ) =>
        NewFlatSaleResolver.isNewFlatSale(category, primarySaleV2, flatType, dealStatus) shouldEqual expected
    }
  }
}
