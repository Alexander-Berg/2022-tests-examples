package ru.auto.salesman.tasks.tradein

import cats.data.NonEmptyList
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, Matchers}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.dao.TradeInRequestDao
import ru.auto.salesman.dao.TradeInRequestDao.Filter.WithNewStatus
import ru.auto.salesman.dao.TradeInRequestDao.Patch.SetBillingStatus
import ru.auto.salesman.dao.TradeInRequestDao.PatchFilter.WithRequestId
import ru.auto.salesman.dao.TradeInRequestDao.{Filter, TradeInRequestCoreData}
import ru.auto.salesman.dao.impl.jdbc.StaticQueryBuilderHelper.{LimitOffset, OrderBy}
import ru.auto.salesman.model.TradeInRequest.{Statuses => BillingStatuses}
import ru.auto.salesman.model._
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.tasks.TradeInRequestsCheckStatusesTask
import ru.auto.salesman.tasks.TradeInRequestsCheckStatusesTask.NewRequestUpdate
import ru.auto.salesman.tasks.tradein.TradeInRequestsCheckStatusesTaskSpec._
import ru.auto.salesman.test.BaseSpec

class TradeInRequestsCheckStatusesTaskSpec
    extends BaseSpec
    with ScalaFutures
    with Matchers
    with BeforeAndAfter {

  private val tradeInRequestDao = mock[TradeInRequestDao]

  val task = new TradeInRequestsCheckStatusesTask(tradeInRequestDao)

  private val getRequests = toMockFunction3 {
    tradeInRequestDao.find(
      _: List[Filter],
      _: Option[OrderBy],
      _: Option[LimitOffset]
    )
  }

  private val updateRequestBillingStatus = toMockFunction2 {
    tradeInRequestDao.update(
      _: TradeInRequestDao.Patch,
      _: NonEmptyList[TradeInRequestDao.PatchFilter]
    )
  }

  "TradeInRequestsBillingTask.execute()" should {
    "put to bill queue trade-in request" in {
      val newRequests = List(
        testTradeInRequest(Category.CARS, Section.NEW, BillingStatuses.New)
      )

      val paidRequests = Nil

      getRequests
        .expects(List(WithNewStatus), None, None)
        .returningT(newRequests)

      getRequests
        .expects(*, None, None)
        .returningT(paidRequests)

      updateRequestBillingStatus
        .expects(*, *)
        .returningT(())

      task
        .execute()
        .success
        .value shouldBe unit
    }
  }

  "TradeInRequestsBillingTask.processClient()" should {
    "put to bill queue only one new request if there is duplicate with different create date" in {
      val newRequests = List(
        testTradeInRequest(Category.CARS, Section.NEW, BillingStatuses.New)
          .copy(id = 1L, createDate = DateTime.now().minusMinutes(10)),
        testTradeInRequest(Category.CARS, Section.NEW, BillingStatuses.New)
          .copy(id = 2L, createDate = DateTime.now().minusMinutes(5))
      )

      val paidRequests = Nil

      updateRequestBillingStatus
        .expects(
          SetBillingStatus(BillingStatuses.Pending),
          NonEmptyList.of(WithRequestId(1L))
        )
        .returningT(())

      updateRequestBillingStatus
        .expects(
          SetBillingStatus(BillingStatuses.Free),
          NonEmptyList.of(WithRequestId(2L))
        )
        .returningT(())

      task
        .processClient(newRequests, paidRequests)
        .success
        .value shouldBe unit
    }
  }

  "TradeInRequestsBillingTask.newRequestsUpdates()" should {
    "not put to bill queue already paid trade-in request" in {
      val paidRequests = List(
        testTradeInRequest(Category.CARS, Section.NEW, BillingStatuses.Paid)
          .copy(id = 1L, createDate = DateTime.now().minusMinutes(10))
      )

      val newRequests = List(
        testTradeInRequest(Category.CARS, Section.NEW, BillingStatuses.New)
          .copy(id = 2L, createDate = DateTime.now().minusMinutes(5)),
        testTradeInRequest(Category.CARS, Section.NEW, BillingStatuses.New)
          .copy(id = 3L, createDate = DateTime.now().minusMinutes(3))
      )

      val result = task.newRequestsUpdates(newRequests, paidRequests)

      result should contain theSameElementsAs List(
        NewRequestUpdate(id = 2L, billingStatus = BillingStatuses.Free),
        NewRequestUpdate(id = 3L, billingStatus = BillingStatuses.Free)
      )
    }

    "put to bill queue all requests if they are from users with different phone numbers" in {
      val request1 =
        testTradeInRequest(Category.CARS, Section.NEW, BillingStatuses.New)
          .copy(
            id = 1L,
            userPhone = userPhone1,
            createDate = DateTime.now().minusMinutes(5)
          )
      val request2 = request1.copy(
        id = 2L,
        userPhone = userPhone2,
        createDate = DateTime.now().minusMinutes(3)
      )
      val newRequests = List(request1, request2)

      val paidRequests = Nil

      val result = task.newRequestsUpdates(newRequests, paidRequests)

      result should contain theSameElementsAs List(
        NewRequestUpdate(id = 1L, billingStatus = BillingStatuses.Pending),
        NewRequestUpdate(id = 2L, billingStatus = BillingStatuses.Pending)
      )
    }

    "put to bill queue only one new request if create dates of requests are the same" in {
      val request1 =
        testTradeInRequest(Category.CARS, Section.NEW, BillingStatuses.New)
          .copy(id = 1L, createDate = DateTime.now().minusMinutes(10))
      val request2 = request1.copy(id = 2L)
      val newRequests = List(request1, request2)

      val paidRequests = Nil

      val result = task.newRequestsUpdates(newRequests, paidRequests)

      result should contain theSameElementsAs List(
        NewRequestUpdate(id = 1L, billingStatus = BillingStatuses.Pending),
        NewRequestUpdate(id = 2L, billingStatus = BillingStatuses.Free)
      )
    }

    "put to bill queue only one new request if create dates of requests are the same and there is one more request between duplicates" in {
      val request1 =
        testTradeInRequest(Category.CARS, Section.NEW, BillingStatuses.New)
          .copy(
            id = 1L,
            userPhone = userPhone1,
            createDate = DateTime.now().minusMinutes(10)
          )
      val request2 = request1.copy(id = 2L, userPhone = userPhone2)
      val request3 = request1.copy(id = 3L)
      val newRequests = List(request1, request2, request3)

      val paidRequests = Nil

      val result = task.newRequestsUpdates(newRequests, paidRequests)

      result should contain theSameElementsAs List(
        NewRequestUpdate(id = 1L, billingStatus = BillingStatuses.Pending),
        NewRequestUpdate(id = 2L, billingStatus = BillingStatuses.Pending),
        NewRequestUpdate(id = 3L, billingStatus = BillingStatuses.Free)
      )
    }
  }

}

object TradeInRequestsCheckStatusesTaskSpec {

  val TestClientId: ClientId = 20101

  val TestTradeInRequestId = 777L

  val userPhone1 = "+78005553535"
  val userPhone2 = "+74959999999"

  def testTradeInRequest(
      category: Category,
      section: Section,
      billingStatus: TradeInRequest.Status = BillingStatuses.New
  ) =
    TradeInRequestCoreData(
      TestTradeInRequestId,
      TestClientId,
      Some(OfferIdentity("111-asdf")),
      category,
      section,
      None,
      Some(OfferIdentity("222-dsa")),
      Some(Category.CARS),
      "+70000000000",
      Some("Vasya"),
      billingStatus,
      0L,
      DateTime.now()
    )

}
