package ru.yandex.vertis.billing.dao

import java.util.concurrent.{CyclicBarrier, Executors}
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.billing.balance.model.{OrderId => BalanceOrderId, ProductId, ServiceId, ServiceOrderId}
import ru.yandex.vertis.billing.model_core.OrderTransactions.Incoming
import ru.yandex.vertis.billing.model_core.{Funds, OrderBalance2, OrderProperties, TotalIncomesFromBeginningsRequest}
import ru.yandex.vertis.billing.service.BalanceApi.OrderInfo
import ru.yandex.vertis.billing.service.OrderService.GetTransactionFilter.ModifiedSince
import ru.yandex.vertis.billing.service.OrderService.GetFilter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
  * Specs on concurrent incomings for [[OrderDao]].
  */
trait OrderDao4Spec extends OrderDaoSpecBase with ScalaFutures {

  /**
    * Default value for futures [[PatienceConfig]].
    */
  private val DefaultPatienceConfig =
    PatienceConfig(Span(60, Seconds), Span(1, Seconds))

  implicit override def patienceConfig: PatienceConfig =
    DefaultPatienceConfig

  "OrderDao (parallel income part)" should {
    "be consistent if insert two incomes in parallel" in {
      val Customer = customerDao.create(customer).get.id

      val TestServiceId: ServiceId = 99
      val TestOrderId: ServiceOrderId = 17029L
      val TestProductId: ProductId = 1L
      val TestIncome: Funds = 236000L

      val Info = OrderInfo(
        BalanceOrderId(TestServiceId, TestOrderId),
        TestProductId,
        TestIncome,
        0L
      )

      orderDao.attach(Customer, Info.id.serviceOrderId, OrderProperties("-", None, actText = None)).get

      val parties = 100
      val ExpectedTotalIncome = TestIncome * parties
      val Requests = Range.inclusive(1, parties).map { i =>
        TotalIncomesFromBeginningsRequest(
          TestServiceId,
          i.toString,
          TestOrderId,
          TestIncome * i
        )
      }

      implicit val ec = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(
          parties,
          new ThreadFactoryBuilder()
            .setNameFormat("OrderDao4Spec-%d")
            .build()
        )
      )

      val barrier = new CyclicBarrier(parties)

      val futures =
        Requests.map(r =>
          Future {
            barrier.await()
            orderDao.totalIncome(r)
          }
        )

      val r = Future.sequence(futures).futureValue
      info(r.size.toString)

      orderDao.get(GetFilter.ForOrderIds(TestOrderId)) match {
        case Success(os) if os.size == 1 =>
          os.head.id should be(TestOrderId)
          os.head.balance2 should be(OrderBalance2(ExpectedTotalIncome, 0L, 0L))
        case other => fail(s"Unexpected $other")
      }
    }
  }
}
