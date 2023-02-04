package ru.auto.salesman.service

import cats.data.NonEmptyList
import cats.syntax.option._
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.api.ResponseModel.VinHistoryPaymentStatusResponse.{
  PaymentStatus => VinHistoryPaymentStatus
}
import ru.auto.salesman.dao.VinHistoryDao
import ru.auto.salesman.dao.VinHistoryDao.Filter.{Active, ForUser, ForVin}
import ru.auto.salesman.dao.VinHistoryDao.{Filter, Record}
import ru.auto.salesman.dao.impl.jdbc.StaticQueryBuilderHelper.{
  LimitOffset,
  Order,
  OrderBy
}
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.ApiModel.{
  VinHistoryBoughtReport,
  VinHistoryBoughtReports,
  VinHistoryPurchaseRecord,
  VinHistoryPurchasesForVin
}
import ru.auto.salesman.model.{AutoruDealer, ClientId}
import ru.auto.salesman.service.VinHistoryService.VinHistoryFilter
import ru.auto.salesman.service.impl.VinHistoryServiceImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.{AutomatedContext, Page, RequestContext}
import ru.yandex.vertis.paging.{Paging, Slice}

class VinHistoryServiceSpec extends BaseSpec {

  private val vinHistoryDao = mock[VinHistoryDao]

  private val vinHistoryService = new VinHistoryServiceImpl(vinHistoryDao)

  implicit private val rc: RequestContext = AutomatedContext(
    "VinHistoryServiceSpec"
  )

  private val getVinHistoryMock = toMockFunction3 {
    vinHistoryDao.get(
      _: NonEmptyList[Filter],
      _: Option[OrderBy],
      _: Option[LimitOffset]
    )
  }

  private val countVinHistoryMock = toMockFunction1 {
    vinHistoryDao.count(_: NonEmptyList[Filter])
  }

  private def forUserAndVinActiveFilter(clientId: ClientId, vin: String) =
    NonEmptyList.of(ForUser(AutoruDealer(clientId)), ForVin(vin), Active)

  "VinHistoryService.checkVinHistory()" should {
    "return PAID if vin history record exists" in {
      val clientId = 123
      val vin = "vin"

      getVinHistoryMock
        .expects(forUserAndVinActiveFilter(clientId, vin), None, None)
        .returningT(
          Iterable(
            Record(
              1,
              AutoruDealer(123),
              "vin",
              None,
              Some("id"),
              DateTime.now(),
              123,
              None
            )
          )
        )

      val result = vinHistoryService.checkVinHistory(clientId, vin).get
      result shouldBe VinHistoryPaymentStatus.PAID
    }

    "return NOT_PAID if no vin history record found" in {
      val clientId = 123
      val vin = "vin"

      getVinHistoryMock
        .expects(forUserAndVinActiveFilter(clientId, vin), None, None)
        .returningT(Nil)

      val result = vinHistoryService.checkVinHistory(clientId, vin).get
      result shouldBe VinHistoryPaymentStatus.NOT_PAID
    }
  }

  "VinHistoryService.getVinHistoryBoughtReports()" should {
    "return dealer reports" in {
      val now = DateTime.now()

      val records = Iterable(
        Record(
          primaryKeyId = 1,
          AutoruDealer(123),
          "vin-1",
          offerId = Some(AutoruOfferId("123-fff")),
          holdId = Some("hold-id-1"),
          deadline = now.plusMinutes(10),
          epoch = 123,
          None
        )
      )

      val searchFilter =
        forUserAndVinActiveFilter(clientId = 123, vin = "vin-1")
      val paging = Page(number = 3, size = 15)

      val ordering = Some(OrderBy("epoch", Order.Desc))
      val limit = Some(LimitOffset(paging))

      getVinHistoryMock
        .expects(searchFilter, ordering, limit)
        .returningT(records)

      countVinHistoryMock
        .expects(searchFilter)
        .returningT(45)

      val filter = VinHistoryFilter(
        vin = Some("vin-1"),
        offerId = None,
        createdFrom = None,
        createdTo = None,
        onlyActive = true
      )

      val expectedResult =
        VinHistoryBoughtReports
          .newBuilder()
          .setPaging {
            Paging
              .newBuilder()
              .setTotal(45)
              .setPageCount(3)
              .setPage {
                Slice.Page
                  .newBuilder()
                  .setNum(3)
                  .setSize(15)
              }
          }
          .addReports {
            VinHistoryBoughtReport
              .newBuilder()
              .setCreatedAt(Timestamps.fromMillis(123))
              .setDeadline(Timestamps.fromMillis(now.plusMinutes(10).getMillis))
              .setVin("vin-1")
              .setOfferId("123-fff")
          }

      vinHistoryService
        .getVinHistoryBoughtReports(clientId = 123, filter, paging)
        .success
        .value shouldBe expectedResult.build()
    }
  }

  "VinHistoryService.getVinHistoryPurchasesByVin()" should {
    def givenDatabaseRecord(
        givenVin: String,
        givenHoldId: Option[String] = None
    ) =
      Record(
        primaryKeyId = 1,
        AutoruDealer(123),
        givenVin,
        offerId = None,
        holdId = givenHoldId,
        deadline = DateTime.parse("2021-06-01T15:15:15.000+03:00"),
        epoch = 123,
        None
      )

    "return vin history purchases" in {

      val givenVin = "vin-1"

      getVinHistoryMock
        .expects(NonEmptyList.of(ForVin(givenVin)), None, None)
        .returningT(
          Iterable(
            givenDatabaseRecord(givenVin, givenHoldId = "hold-id-1".some)
          )
        )

      val expected =
        VinHistoryPurchasesForVin
          .newBuilder()
          .setVin("vin-1")
          .addRecords(
            VinHistoryPurchaseRecord
              .newBuilder()
              .setUserId("dealer:123")
              .setHoldId("hold-id-1")
              .setCreatedAt(Timestamps.fromNanos(123000000))
              .setDeadline(Timestamps.fromSeconds(1622549715))
          )
          .build()

      vinHistoryService
        .getVinHistoryPurchasesByVin(vin = givenVin)
        .success
        .value shouldBe expected
    }

    "map record if hold_id is not present" in {
      val givenVin = "vin-1"

      getVinHistoryMock
        .expects(NonEmptyList.of(ForVin(givenVin)), None, None)
        .returningT(Iterable(givenDatabaseRecord(givenVin)))

      val expected =
        VinHistoryPurchasesForVin
          .newBuilder()
          .setVin("vin-1")
          .addRecords(
            VinHistoryPurchaseRecord
              .newBuilder()
              .setUserId("dealer:123")
              .setCreatedAt(Timestamps.fromNanos(123000000))
              .setDeadline(Timestamps.fromSeconds(1622549715))
          )
          .build()

      vinHistoryService
        .getVinHistoryPurchasesByVin(vin = givenVin)
        .success
        .value shouldBe expected
    }
  }
}
