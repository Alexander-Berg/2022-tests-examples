package ru.yandex.vertis.billing.tasks

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.MonthlyDiscountDao
import ru.yandex.vertis.billing.model_core.FundsConversions._
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{
  orderTransactionGen,
  rebateRequestGen,
  CustomerIdGen,
  OrderGen,
  OrderTransactionGenParams,
  Producer
}
import ru.yandex.vertis.billing.service.OrderService.{GetFilter, TransactionsOutcomeQuery}
import ru.yandex.vertis.billing.service.{MonthlyDiscountService, OrderService}
import ru.yandex.vertis.billing.util.{DateTimeUtils, RequestContext}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.util.{Random, Success}

/**
  * Spec on [[MonthlyDiscountTask]]
  *
  * @author ruslansd
  */
class MonthlyDiscountTaskSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with MockitoSupport {

  import MonthlyDiscountTaskSpec._

  override protected def beforeEach(): Unit = {
    clearInvocations[Any](MonthlyDiscountServiceMock, OrderServiceMock)
    reset[Any](MonthlyDiscountServiceMock, OrderServiceMock)
    stub(OrderServiceMock.get(_: GetFilter)(_: RequestContext)) { case _ =>
      Success(Iterable(order))
    }
    super.beforeEach()
  }

  private val customerId = CustomerIdGen.next
  private val order = OrderGen.next.copy(owner = customerId)

  private val rebateResponse = RebateResponse(
    orderTransactionGen(OrderTransactionGenParams().withType(OrderTransactions.Rebate)).next.asInstanceOf[Rebate],
    rebateRequestGen(order.id).next,
    order
  )

  private val task = new AlwaysRunDiscountTask(MonthlyDiscountServiceMock, OrderServiceMock)

  def mockOutcome(funds: Funds) =
    OrderTransactionsOutcome(0, funds, 0, 0, 0)

  def mockDiscount(
      customer: CustomerId = customerId,
      effectiveSince: DateTime = DateTimeUtil.now()): MonthlyDiscountDao.Record =
    MonthlyDiscountDao.Record(customer, effectiveSince)

  "MonthlyDiscountTask" should {
    "correctly work on empty set" in {
      when(MonthlyDiscountServiceMock.get(?)(?))
        .thenReturn(Success(Iterable.empty))

      (task.execute() should be).a(Symbol("Success"))
      verify(MonthlyDiscountServiceMock).get(?)(?)
      verifyNoInteractions(OrderServiceMock)
    }

    "no discount if it not enough amount" in {
      when(MonthlyDiscountServiceMock.get(?)(?))
        .thenReturn(Success(Iterable(mockDiscount())))

      when(OrderServiceMock.getTransactionsOutcome(?)(?))
        .thenReturn(Success(mockOutcome(400 * 1000 * 100)))

      (task.execute() should be).a(Symbol("Success"))

      verify(MonthlyDiscountServiceMock).get(?)(?)
      verify(OrderServiceMock).get(?[GetFilter])(?)
      verify(OrderServiceMock).getTransactionsOutcome(?)(?)
      verify(OrderServiceMock, times(0)).execute2(?)(?)
    }

    "provide rebate if enough month 10%" in {
      when(MonthlyDiscountServiceMock.get(?)(?))
        .thenReturn(Success(Iterable(mockDiscount())))

      val amount = 500.thousands
      val discount = 50.thousands
      when(OrderServiceMock.getTransactionsOutcome(?)(?))
        .thenReturn(Success(mockOutcome(amount)))

      stub(OrderServiceMock.execute2(_: OrderTransactionRequest)(_: RequestContext)) {
        case (req: RebateRequest, _) if req.amount == discount =>
          Success(rebateResponse)
      }

      (task.execute() should be).a(Symbol("Success"))

      verify(MonthlyDiscountServiceMock).get(?)(?)
      verify(OrderServiceMock).get(?[GetFilter])(?)
      verify(OrderServiceMock).getTransactionsOutcome(?)(?)
      verify(OrderServiceMock).execute2(?)(?)
    }

    "provide rebate if enough month 15%" in {
      when(MonthlyDiscountServiceMock.get(?)(?))
        .thenReturn(Success(Iterable(mockDiscount())))

      val amount = 1.million
      val discount = 150.thousands
      when(OrderServiceMock.getTransactionsOutcome(?)(?))
        .thenReturn(Success(mockOutcome(amount)))

      stub(OrderServiceMock.execute2(_: OrderTransactionRequest)(_: RequestContext)) {
        case (req: RebateRequest, _) if req.amount == discount =>
          Success(rebateResponse)
      }

      (task.execute() should be).a(Symbol("Success"))

      verify(MonthlyDiscountServiceMock).get(?)(?)
      verify(OrderServiceMock).get(?[GetFilter])(?)
      verify(OrderServiceMock).getTransactionsOutcome(?)(?)
      verify(OrderServiceMock).execute2(?)(?)
    }

    "correct discount amount" in {
      MonthlyDiscountTask.discount(1.thousands) shouldBe None
      MonthlyDiscountTask.discount(0) shouldBe None
      MonthlyDiscountTask.discount(501.thousands) shouldBe Some(PercentDiscount(10000))
      MonthlyDiscountTask.discount(999.thousands) shouldBe Some(PercentDiscount(10000))
      MonthlyDiscountTask.discount(1.million) shouldBe Some(PercentDiscount(15000))
      MonthlyDiscountTask.discount(2.million) shouldBe Some(PercentDiscount(15000))
    }

    "correct skip not executable intervals" in {
      val month = DateTimeUtils.wholeMonth(DateTimeUtils.now())
      MonthlyDiscountTask.isLastHourOfCurrentMonth(month.to.minusHours(5)) shouldBe false
      MonthlyDiscountTask.isLastHourOfCurrentMonth(month.to.minusDays(5)) shouldBe false
      MonthlyDiscountTask.isLastHourOfCurrentMonth(month.to.minusWeeks(2)) shouldBe false
      MonthlyDiscountTask.isLastHourOfCurrentMonth(month.from) shouldBe false
      MonthlyDiscountTask.isLastHourOfCurrentMonth(month.to.minusMinutes(10)) shouldBe true
      MonthlyDiscountTask.isLastHourOfCurrentMonth(month.to) shouldBe true
    }

    "correct work with agency clients (case 1)" in {
      val agencyId = customerId.copy(agencyId = Some(customerId.clientId))
      when(MonthlyDiscountServiceMock.get(?)(?))
        .thenReturn(Success(Iterable(mockDiscount(agencyId))))

      val orders = for (i <- 1 to 5) yield OrderGen.next.copy(id = i)
      stub(OrderServiceMock.get(_: GetFilter)(_: RequestContext)) { case _ =>
        Success(orders)
      }

      val amount = 100.thousands
      val discount = 10.thousands
      when(OrderServiceMock.getTransactionsOutcome(?)(?))
        .thenReturn(Success(mockOutcome(amount)))

      stub(OrderServiceMock.execute2(_: OrderTransactionRequest)(_: RequestContext)) {
        case (req: RebateRequest, _) if req.amount == discount =>
          Success(rebateResponse)
      }

      (task.execute() should be).a(Symbol("Success"))

      verify(MonthlyDiscountServiceMock).get(?)(?)
      verify(OrderServiceMock).get(?[GetFilter])(?)
      verify(OrderServiceMock, times(5)).getTransactionsOutcome(?)(?)
      verify(OrderServiceMock, times(5)).execute2(?)(?)
    }

    "correct work with agency clients (case 2)" in {
      val agencyId = customerId.copy(agencyId = Some(customerId.clientId))
      when(MonthlyDiscountServiceMock.get(?)(?))
        .thenReturn(Success(Iterable(mockDiscount(agencyId))))
      val orders = for (i <- 1 to 5) yield OrderGen.next.copy(id = i)
      stub(OrderServiceMock.get(_: GetFilter)(_: RequestContext)) { case _ =>
        Success(orders)
      }

      val outcomes = orders.map { o =>
        val outcome = mockOutcome(Random.nextInt(100).thousands + 100.thousands)
        o.id -> outcome
      }.toMap

      val discount = MonthlyDiscountTask.discount(outcomes.values.map(_.withdraw).sum).get

      stub(OrderServiceMock.getTransactionsOutcome(_: TransactionsOutcomeQuery)(_: RequestContext)) { case (query, _) =>
        Success(outcomes(query.orderId))
      }

      stub(OrderServiceMock.execute2(_: OrderTransactionRequest)(_: RequestContext)) {
        case (req: RebateRequest, _) if discount.calc(outcomes(req.orderId).withdraw) == req.amount =>
          Success(rebateResponse)
      }

      (task.execute() should be).a(Symbol("Success"))

      verify(MonthlyDiscountServiceMock).get(?)(?)
      verify(OrderServiceMock).get(?[GetFilter])(?)
      verify(OrderServiceMock, times(5)).getTransactionsOutcome(?)(?)
      verify(OrderServiceMock, times(5)).execute2(?)(?)
    }

    "correct work with client with few orders" in {
      when(MonthlyDiscountServiceMock.get(?)(?))
        .thenReturn(Success(Iterable(mockDiscount())))
      val orders = for (i <- 1 to 5) yield OrderGen.next.copy(id = i)
      stub(OrderServiceMock.get(_: GetFilter)(_: RequestContext)) { case _ =>
        Success(orders)
      }

      val outcomes = orders.map { o =>
        val outcome = mockOutcome(Random.nextInt(100).thousands + 100.thousands)
        o.id -> outcome
      }.toMap

      val discount = MonthlyDiscountTask.discount(outcomes.values.map(_.withdraw).sum).get

      stub(OrderServiceMock.getTransactionsOutcome(_: TransactionsOutcomeQuery)(_: RequestContext)) { case (query, _) =>
        Success(outcomes(query.orderId))
      }

      stub(OrderServiceMock.execute2(_: OrderTransactionRequest)(_: RequestContext)) {
        case (req: RebateRequest, _) if discount.calc(outcomes(req.orderId).withdraw) == req.amount =>
          Success(rebateResponse)
      }

      (task.execute() should be).a(Symbol("Success"))

      verify(MonthlyDiscountServiceMock).get(?)(?)
      verify(OrderServiceMock).get(?[GetFilter])(?)
      verify(OrderServiceMock, times(5)).getTransactionsOutcome(?)(?)
      verify(OrderServiceMock, times(5)).execute2(?)(?)
    }

  }

}

object MonthlyDiscountTaskSpec {
  import MockitoSupport._

  class AlwaysRunDiscountTask(md: MonthlyDiscountService, os: OrderService) extends MonthlyDiscountTask(md, os) {
    override protected def isTimeToExecute(time: DateTime): Boolean = true
  }

  val MonthlyDiscountServiceMock = mock[MonthlyDiscountService]
  val OrderServiceMock = mock[OrderService]
}
