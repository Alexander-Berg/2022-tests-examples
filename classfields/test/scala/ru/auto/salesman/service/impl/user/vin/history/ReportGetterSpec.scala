package ru.auto.salesman.service.impl.user.vin.history

import cats.data.NonEmptyList
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.salesman.dao.VinHistoryDao
import ru.auto.salesman.dao.VinHistoryDao.Filter._
import ru.auto.salesman.dao.VinHistoryDao.Record
import ru.auto.salesman.dao.impl.jdbc.StaticQueryBuilderHelper.{
  LimitOffset,
  Order,
  OrderBy
}
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.ApiModel.{
  VinHistoryBoughtReport,
  VinHistoryBoughtReports
}
import ru.auto.salesman.service.user.SubscriptionService.VinHistoryFilter
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.{AutomatedContext, Page}
import ru.yandex.vertis.paging.{Paging, Slice}

class ReportGetterSpec extends BaseSpec {

  implicit private val rc = AutomatedContext("test")

  val vinHistoryDao: VinHistoryDao = mock[VinHistoryDao]

  val service =
    new ReportGetter(vinHistoryDao)
  "SubscriptionServiceImpl.getVinHistoryBoughtReports()" should {
    "return user reports" in {
      val now = DateTime.now()

      val user = AutoruUser("user:123")
      val offerId = AutoruOfferId("123-fff")
      val createdFrom = now.minusDays(30)

      val records = Iterable(
        Record(
          primaryKeyId = 1,
          user,
          "vin-1",
          Some(offerId),
          holdId = Some("hold-id-1"),
          deadline = now.plusMinutes(10),
          epoch = 123,
          None
        )
      )

      val searchFilter = NonEmptyList.of(
        ForUser(user),
        ForVin("vin-1"),
        ForOfferId(offerId),
        CreatedFrom(createdFrom),
        Active
      )

      val paging = Page(number = 3, size = 15)

      val ordering = Some(OrderBy("epoch", Order.Desc))
      val limit = Some(LimitOffset(paging))

      (vinHistoryDao.get _)
        .expects(searchFilter, ordering, limit)
        .returningT(records)
        .atLeastOnce()

      (vinHistoryDao.count _)
        .expects(searchFilter)
        .returningT(45)
        .atLeastOnce()

      val filter = VinHistoryFilter(
        vin = Some("vin-1"),
        offerId = Some(offerId),
        createdFrom = Some(createdFrom),
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

      service
        .getReports("user:123", filter, paging)
        .success
        .value shouldBe expectedResult.build()
    }
  }
}
