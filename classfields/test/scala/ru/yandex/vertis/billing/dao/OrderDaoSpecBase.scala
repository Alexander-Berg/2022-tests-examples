package ru.yandex.vertis.billing.dao

import billing.CommonModel
import billing.emon.Model.{Event => _, EventState, _}
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.gens.ValidEmonEventStateGen
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{
  CustomerHeaderGen,
  OrderPropertiesGen,
  Producer,
  ProtobufMessageGenerators,
  WithdrawRequest2Gen
}
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import ru.yandex.vertis.protobuf.ProtoInstanceProvider.defaultInstanceByType

import java.util.concurrent.atomic.AtomicInteger
import scala.util.Success

/**
  * Base stuff for specs on [[OrderDao]].
  *
  * @author dimas
  */
trait OrderDaoSpecBase extends AnyWordSpec with Matchers {

  val incomingIds = new AtomicInteger

  protected def customerDao: CustomerDao

  protected def orderDao: OrderDao

  val customer: CustomerHeader = CustomerHeaderGen.next

  val orders = scala.collection.mutable.Set.empty[Order]

  protected def createOrder(amount: Option[Funds] = None, customerId: Option[CustomerId] = None): Order = {
    val c = customer.copy(id = customerId.getOrElse(customer.id))
    customerDao.create(c)
    val properties = OrderPropertiesGen.next

    val order = orderDao.create(c.id, properties) match {
      case Success(created) =>
        orders += created
        created.properties should be(properties)
        created.balance2 should be(OrderBalance2.empty)
        created
      case other =>
        fail(s"Unable to create order: $other")
    }

    amount.foreach { value =>
      val id = incomingIds.incrementAndGet().toString
      orderDao.totalIncome(TotalIncomesFromBeginningsRequest(1, id, order.id, value))
    }

    order
  }

  protected def withdrawRequest(time: DateTime, o: OrderId, amount: Funds): WithdrawRequest2 = {
    val w = WithdrawRequest2Gen(o).next
    w.copy(snapshot = w.snapshot.copy(time = time), amount = amount)
  }

  protected def withdrawRequest(o: OrderId, amount: Funds): WithdrawRequest2 =
    withdrawRequest(now(), o, amount)

  protected def customEventStateGen(orderId: OrderId, price: Funds, eventStateId: EventStateId): Gen[EventState] = {
    ValidEmonEventStateGen
      .map { e =>
        val b = e.toBuilder
        b.getEventBuilder.setEventId(eventStateId.getEventId)
        b.setSnapshotId(eventStateId.getSnapshotId)
        b.getEventBuilder.getPayerBuilder.getBalanceBuilder.setOrderId(orderId)
        b.getPriceBuilder.getResponseBuilder.getRuleBuilder.getPriceBuilder.setKopecks(price)
        b.build()
      }
  }

  protected val fixedProductEventGen: Gen[EventStateId] =
    ProtobufMessageGenerators.generate[EventStateId](depth = 5).map { e =>
      val b = e.toBuilder
      b.getEventIdBuilder.setEventType(EventTypeNamespace.EventType.REALTY_DEVCHAT)
      b.getEventIdBuilder.setProject(CommonModel.Project.REALTY)
      b.build()
    }

}
