package ru.yandex.vertis.billing.tasks

import com.google.protobuf.MessageOrBuilder
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.BillingEvent.TransactionBillingInfo
import ru.yandex.vertis.billing.dao.OrderDao
import ru.yandex.vertis.billing.logging.ProtoJsonLogger
import ru.yandex.vertis.billing.model_core.gens.{orderTransactionGen, OrderGen, OrderTransactionGenParams, Producer}
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.EpochService
import ru.yandex.vertis.billing.{BillingEvent, SupportedServices}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success}

/**
  * @author ruslansd
  */
class TransactionLogProcessingTaskSpec extends AnyWordSpec with Matchers with MockitoSupport with BeforeAndAfterEach {

  private val loggedTransactions = ArrayBuffer.empty[TransactionBillingInfo]

  override def beforeEach(): Unit = {
    super.beforeEach()
    loggedTransactions.clear()
    Mockito.clearInvocations(epochServiceMock)
  }

  private val epochServiceMock = {
    val m = mock[EpochService]

    when(m.getTry(?)).thenReturn(Success(0L))
    stub(m.setTry _) { case (_, _) =>
      Success(())
    }
    m
  }

  private def orderMock(trs: Iterable[OrderTransaction], orders: Iterable[Order]) = {
    val m = mock[OrderDao]

    when(m.getTransactions(?, ?))
      .thenReturn(Success(trs))

    when(m.get(?, ?))
      .thenReturn(Success(orders))

    m
  }

  def task(trs: Iterable[OrderTransaction], orders: Iterable[Order], domain: String): TransactionLogProcessingTask =
    task(orderMock(trs, orders), domain)

  def task(orderDao: OrderDao, domain: String): TransactionLogProcessingTask =
    new TransactionLogProcessingTask(orderDao, epochServiceMock, domain) with ProtoJsonLogger {

      override protected def logProtoJson(msg: MessageOrBuilder): Unit = {
        loggedTransactions += msg.asInstanceOf[TransactionBillingInfo]
      }

    }

  private def checkTransactions(
      trs: Iterable[OrderTransaction],
      orders: Seq[Order],
      domain: BillingEvent.Domain): Unit = {
    loggedTransactions.size shouldBe trs.size
    val transactionsMap = trs.groupBy(_.id).view.mapValues(_.head).toMap
    val ordersMap = orders.groupBy(_.id).view.mapValues(_.head).toMap
    loggedTransactions.foreach { loggedTransaction =>
      val connectedTr = transactionsMap(loggedTransaction.getTrId)
      loggedTransaction.getTimestamp shouldBe connectedTr.timestamp.getMillis
      loggedTransaction.getDomain shouldBe domain
      loggedTransaction.getStateEpoch shouldBe connectedTr.epoch.getOrElse(connectedTr.timestamp.getMillis)
      val connectedOrder = ordersMap(loggedTransaction.getOrderState.getId)
      val expectedRawTrMessage = Conversions.toMessage(connectedTr).get
      val correctCustomerId = Conversions.toMessage(connectedOrder.owner)
      val expectedTrMessage = expectedRawTrMessage.toBuilder.setCustomerId(correctCustomerId).build()
      connectedTr match {
        case _: Incoming =>
          val tr = loggedTransaction.getIncoming.getTransaction
          tr shouldBe expectedTrMessage
        case c: Correction =>
          val correctionMessage = loggedTransaction.getCorrection
          val tr = correctionMessage.getTransaction
          tr shouldBe expectedTrMessage
          val actualReason = correctionMessage.getReason
          actualReason shouldBe c.comment
        case r: Rebate =>
          val rebateMessage = loggedTransaction.getRebate
          val tr = rebateMessage.getTransaction
          tr shouldBe expectedTrMessage
          val actualReason = rebateMessage.getDetails
          actualReason shouldBe r.comment
        case other =>
          fail(s"Unexpected $other")
      }
      val expectedOrderState = Conversions.toOrderStateMessage(connectedOrder)
      loggedTransaction.getOrderState shouldBe expectedOrderState
    }
  }

  "TransactionLogProcessingTask" should {

    "process empty set" in {
      val t = task(Iterable.empty, Iterable.empty, SupportedServices.AutoRu)

      (t.execute() should be).a(Symbol("Success"))
      loggedTransactions shouldBe empty
    }

    "process successfully" in {
      def generateOrderTransaction(trType: OrderTransactions.Value, orderId: Long) =
        orderTransactionGen(OrderTransactionGenParams().withType(trType).withOrderId(orderId)).next

      val incomings = for (i <- 1 to 5) yield generateOrderTransaction(OrderTransactions.Incoming, i)
      val rebates = for (i <- 10 to 15) yield generateOrderTransaction(OrderTransactions.Rebate, i)
      val corrections = for (i <- 20 to 25) yield generateOrderTransaction(OrderTransactions.Correction, i)

      val trs = incomings ++ rebates ++ corrections

      val orders = trs.map { t =>
        val order = OrderGen.next
        order.copy(id = t.orderId)
      }
      val t = task(trs, orders, SupportedServices.AutoRu)
      (t.execute() should be).a(Symbol("Success"))

      checkTransactions(trs, orders.toSeq, BillingEvent.Domain.AUTORU)
    }

    "handle order service fail" in {
      val orderDao = orderMock(Iterable.empty, Iterable.empty)

      when(orderDao.getTransactions(?, ?))
        .thenReturn(Failure(new RuntimeException("artificial")))

      val t = task(orderDao, SupportedServices.AutoRu)

      (t.execute() should be).a(Symbol("Failure"))

      Mockito.verify(epochServiceMock).getTry(?)
      Mockito.verify(orderDao, Mockito.times(0)).get(?, ?)
      Mockito.verify(epochServiceMock, Mockito.times(0)).setTry(?, ?)
    }

  }

}
