package ru.auto.salesman.service.async

import org.joda.time.LocalDate
import ru.auto.salesman.dao.OffersWithPaidProductsSalesmanDao
import ru.auto.salesman.dao.OffersWithPaidProductsSalesmanDao.ActivatedForClientInDateIntervalWithLimit
import ru.auto.salesman.model.{OffersWithPaidProducts, Paging}
import ru.auto.salesman.test.{BaseSpec, TestException}

class AsyncOffersResolverSpec extends BaseSpec {

  val offersWithPaidProductsSalesmanDao =
    mock[OffersWithPaidProductsSalesmanDao]

  val asyncOffersResolver = new AsyncOffersResolver(
    offersWithPaidProductsSalesmanDao
  )

  val uniqueOfferIds = List(
    123456L,
    789012L
  )
  val totalCount = 20

  "AsyncOffersResolver" should {
    "return limited offer ids and count" in {
      (offersWithPaidProductsSalesmanDao.getUniqueOfferIds _)
        .expects(
          ActivatedForClientInDateIntervalWithLimit(
            20101,
            List.empty,
            LocalDate.parse("2019-01-01"),
            LocalDate.parse("2019-01-04"),
            1,
            0
          )
        )
        .returningT(List(uniqueOfferIds.head))

      (offersWithPaidProductsSalesmanDao.countUniqueOffers _)
        .expects(*)
        .returningT(totalCount)

      val result = asyncOffersResolver
        .getOffersWithPaidProducts(
          LocalDate.parse("2019-01-01"),
          LocalDate.parse("2019-01-04"),
          20101,
          List.empty,
          Paging(0, 1)
        )
        .futureValue

      result shouldBe OffersWithPaidProducts(
        List(uniqueOfferIds.head),
        totalCount
      )
    }

    "fail if unable to receive records from db" in {
      (offersWithPaidProductsSalesmanDao.getUniqueOfferIds _)
        .expects(*)
        .throwing(new TestException)

      asyncOffersResolver
        .getOffersWithPaidProducts(
          LocalDate.parse("2019-01-01"),
          LocalDate.parse("2019-01-04"),
          20101,
          List.empty,
          Paging(0, 1)
        )
        .failed
        .futureValue shouldBe a[TestException]
    }

    "fail if unable to receive total records count from db" in {
      (offersWithPaidProductsSalesmanDao.getUniqueOfferIds _)
        .expects(*)
        .returningT(List(uniqueOfferIds.head))

      (offersWithPaidProductsSalesmanDao.countUniqueOffers _)
        .expects(*)
        .throwing(new TestException)

      asyncOffersResolver
        .getOffersWithPaidProducts(
          LocalDate.parse("2019-01-01"),
          LocalDate.parse("2019-01-04"),
          20101,
          List.empty,
          Paging(0, 1)
        )
        .failed
        .futureValue shouldBe a[TestException]
    }
  }
}
