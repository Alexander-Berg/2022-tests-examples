package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.impl.jdbc.BillingReportRecord
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{orderTransactionRequestGen, CustomerHeaderGen, OrderGen, Producer}
import ru.yandex.vertis.billing.service.OrderService.{GetFilter, Transparent}
import ru.yandex.vertis.billing.util.{DateTimeInterval, Page, SlicedResult}

import scala.util.{Random, Success}

/**
  * Spec on [[BillingReportDao]]
  *
  * @author ruslansd
  */
trait BillingReportDaoSpec extends AnyWordSpec with Matchers {

  protected def orderDao: OrderDao

  protected def billingReportDao: BillingReportDao

  protected def customerDao: CustomerDao

  val random = new Random()
  val date = now().withTimeAtStartOfDay()
  val customer = CustomerHeaderGen.next

  def nextDate = date.minusDays(random.nextInt(7))

  def nextTransactionId = math.abs(random.nextInt()).toString

  "BillingReportDao" should {
    customerDao.create(customer)
    val count = 5
    val orders = OrderGen.next(count).map(_.copy(owner = customer.id))
    generateTransactions(orders)

    "get transactions for default period" in {
      val week = DateTimeInterval(date.minusDays(7), date)
      val transactions = getTransactions(orders, week)
      billingReportDao.getReport(week).get.toSet should be(generateBillingReportRecords(transactions).toSet)
    }

    "get transactions" in {
      val interval = DateTimeInterval(nextDate, date)
      val transactions = getTransactions(orders, interval)
      billingReportDao.getReport(interval).get.toSet should be(generateBillingReportRecords(transactions).toSet)
    }

  }

  def getTransactions(orders: Iterable[Order], interval: DateTimeInterval) = {
    orders.map(order => {
      orderDao.listTransactions(customer.id, order.id, interval, Transparent, Page(0, 10)) match {
        case Success(tr) =>
          tr
        case other =>
          fail(s"Unable to get transactions $other")
      }
    })
  }

  def generateTransactions(orders: Iterable[Order]) = {
    for (order <- orders) {
      val producer = orderTransactionRequestGen(order.id)
      for (i <- 1 to 10) {
        producer.next match {
          case t: TotalIncomesFromBeginningsRequest =>
            orderDao.totalIncome(t)
          case t: CorrectionRequest =>
            orderDao.correct(nextTransactionId, t.orderId, nextDate, t.amount, t.comment)
          case t: WithdrawRequest2 =>
            orderDao.withdraw2(nextTransactionId, t)
          case t: RebateRequest =>
            orderDao.rebate(nextTransactionId, t)
          case req: EmonTransactionRequest =>
            orderDao.withdrawEvent(req)
        }
      }
    }
  }

  def generateBillingReportRecords(transactions: Iterable[SlicedResult[OrderTransaction]]) = {
    transactions.flatMap(_.iterator.map(t => {
      val orderId = t.orderId
      val date = t.timestamp
      val amount = t.amount
      val clientId = customer.id.clientId
      val comment = t match {
        case request: CorrectionRequest =>
          Some(request.comment)
        case _ =>
          None
      }
      val order = orderDao.get(GetFilter.ForCustomer(customer.id, orderId)) match {
        case Success(r) =>
          r.head
        case other =>
          fail(s"Unable to get order $other")
      }
      val name = t.getType.toString
      BillingReportRecord(
        orderId,
        date,
        amount.toDouble / 100,
        clientId,
        order.properties.text,
        name,
        comment
      )
    }))
  }
}
