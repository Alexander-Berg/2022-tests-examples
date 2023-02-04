package ru.auto.salesman.service.tradein

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.cabinet.TradeInRequest.TradeInRequestForm
import ru.auto.salesman.client.SenderClient
import ru.auto.salesman.dao.TradeInRequestDao.Filter._
import ru.auto.salesman.dao.TradeInRequestDao.TradeInRequestCoreData
import ru.auto.salesman.dao.impl.jdbc.StaticQueryBuilderHelper.{
  LimitOffset,
  Order,
  OrderBy
}
import ru.auto.salesman.dao.{ClientSubscriptionsDao, TradeInRequestDao}
import ru.auto.salesman.model.common.{PageModel, PagingModel}
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.{
  SectionRecordsAvailable,
  TradeInRequest,
  TradeInRequestCore,
  TradeInRequestCoreListing
}
import ru.auto.salesman.service.tradein.TradeInService.UnsupportedTradeInCategorySectionException
import ru.auto.salesman.test.{BaseSpec, TestException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class TradeInServiceSpec extends BaseSpec {

  val tradeInDao = mock[TradeInRequestDao]
  val sender = mock[SenderClient]
  val clientSubscriptionsDao = mock[ClientSubscriptionsDao]

  val tradeInService =
    new TradeInService(tradeInDao, sender, clientSubscriptionsDao)(global)

  "insert" should {
    "fail if invalid category/section" in {
      tradeInService
        .insert(
          TradeInRequest(
            20101,
            TradeInRequestForm
              .newBuilder()
              .setClientOfferInfo(
                TradeInRequestForm.OfferInfo
                  .newBuilder()
                  .setCategory(Category.MOTO)
              )
              .build()
          )
        )
        .failed
        .get shouldBe an[UnsupportedTradeInCategorySectionException]
    }
  }

  "find()" should {

    val clientId = 1L
    val dateFrom = DateTime.parse("2019-01-01")
    val dateTo = DateTime.parse("2019-01-01")
    val pageSize = 10
    val pageNum = 1
    val section = Section.NEW
    val filters = List(
      WithClients(Iterable(clientId)),
      WithNotFailedBilling,
      CreatedSince(dateFrom.toLocalDate),
      CreatedBefore(dateTo.toLocalDate),
      WithSection(section)
    )
    val expectedTradeInRequest = TradeInRequestCore(
      1L,
      clientId,
      Some("1234-567"),
      Some(3214),
      "89175586983",
      Some("vasya"),
      Some("7654-321"),
      10000,
      TradeInRequest.Statuses.Paid,
      DateTime.parse("2019-01-01")
    )
    val responseFromDb = TradeInRequestCoreData(
      1L,
      clientId,
      Some(OfferIdentity("1234-567")),
      Category.CARS,
      Section.NEW,
      Some(3214),
      Some(OfferIdentity("7654-321")),
      Some(Category.CARS),
      "89175586983",
      Some("vasya"),
      TradeInRequest.Statuses.Paid,
      10000,
      DateTime.parse("2019-01-01")
    )
    val expectedResponse = {
      val recordsWithSections = Seq(
        SectionRecordsAvailable(
          Section.NEW,
          available = true
        ),
        SectionRecordsAvailable(
          Section.USED,
          available = false
        )
      )
      val paging = PagingModel(1, 1, PageModel(1, 10))

      TradeInRequestCoreListing(
        Seq(expectedTradeInRequest),
        recordsWithSections,
        10000,
        paging
      )
    }

    "return trade-in requests listing" in {
      (tradeInDao.find _)
        .expects(
          filters,
          Some(OrderBy("create_date", Order.Desc)),
          Some(LimitOffset(Some(pageSize), Some(0)))
        )
        .returns(Success(List(responseFromDb)))
      (tradeInDao.totalCost _)
        .expects(filters)
        .returning(Success(10000))
      (tradeInDao.count _)
        .expects(filters)
        .twice()
        .returns(Success(1))
      (tradeInDao.count _)
        .expects(
          filters.filterNot(_ == WithSection(section)) ++ List(
            WithSection(Section.USED)
          )
        )
        .returns(Success(0))

      val result = tradeInService
        .find(clientId, dateFrom, dateTo, pageNum, pageSize, Some(Section.NEW))
        .futureValue

      result shouldBe expectedResponse
      result.recordsWithSections.map(_.section) shouldBe List(
        Section.NEW,
        Section.USED
      )
    }

    "fail if unable to receive records from db" in {
      (tradeInDao.find _)
        .expects(
          filters,
          Some(OrderBy("create_date", Order.Desc)),
          Some(LimitOffset(Some(pageSize), Some(0)))
        )
        .throws(new TestException)
      (tradeInDao.totalCost _)
        .expects(filters)
        .noMoreThanOnce()
        .returning(Success(10000))
      (tradeInDao.count _)
        .expects(filters)
        .noMoreThanTwice()
        .returns(Success(1))
      (tradeInDao.count _)
        .expects(
          filters.filterNot(_ == WithSection(section)) ++ List(
            WithSection(Section.USED)
          )
        )
        .noMoreThanOnce()
        .returns(Success(0))

      tradeInService
        .find(clientId, dateFrom, dateTo, pageNum, pageSize, Some(Section.NEW))
        .failed
        .futureValue shouldBe a[TestException]
    }

    "fail if unable to receive total cost from db" in {
      (tradeInDao.find _)
        .expects(
          filters,
          Some(OrderBy("create_date", Order.Desc)),
          Some(LimitOffset(Some(pageSize), Some(0)))
        )
        .returns(Success(List(responseFromDb)))
      (tradeInDao.totalCost _)
        .expects(filters)
        .throws(new TestException)
      (tradeInDao.count _)
        .expects(filters)
        .twice()
        .returns(Success(1))
      (tradeInDao.count _)
        .expects(
          filters.filterNot(_ == WithSection(section)) ++ List(
            WithSection(Section.USED)
          )
        )
        .returns(Success(0))

      tradeInService
        .find(clientId, dateFrom, dateTo, pageNum, pageSize, Some(Section.NEW))
        .failed
        .futureValue shouldBe a[TestException]
    }

    "fail if unable to receive count per sections" in {
      (tradeInDao.find _)
        .expects(
          filters,
          Some(OrderBy("create_date", Order.Desc)),
          Some(LimitOffset(Some(pageSize), Some(0)))
        )
        .returns(Success(List(responseFromDb)))
      (tradeInDao.totalCost _)
        .expects(filters)
        .returning(Success(10000))
      (tradeInDao.count _)
        .expects(filters)
        .twice()
        .throws(new TestException)
      (tradeInDao.count _)
        .expects(
          filters.filterNot(_ == WithSection(section)) ++ List(
            WithSection(Section.USED)
          )
        )
        .throws(new TestException)

      tradeInService
        .find(clientId, dateFrom, dateTo, pageNum, pageSize, Some(Section.NEW))
        .failed
        .futureValue shouldBe a[TestException]
    }
  }
}
