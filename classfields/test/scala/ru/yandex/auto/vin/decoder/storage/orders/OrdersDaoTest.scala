package ru.yandex.auto.vin.decoder.storage.orders

import auto.carfax.pro_auto.core.src.testkit.PgDatabaseContainer
import com.google.protobuf.util.Timestamps
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.orders.OrdersApiModel
import ru.auto.api.vin.orders.OrdersApiModel.PublicOrderModel
import ru.auto.api.vin.orders.RequestModel.GetOrdersListRequest.OrdersFilter
import ru.yandex.auto.vin.decoder.manager.orders.OrdersConverter
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.{Order, OrderStatus}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.DateTimeUtils.nowProto
import ru.yandex.auto.vin.decoder.utils.Paging
import auto.carfax.common.utils.protobuf.ProtobufConverterOps.ProtoTimestampOps

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class OrdersDaoTest extends AnyFunSuite with PgDatabaseContainer {

  val dao = new OrdersDao(database, new OrdersConverter)

  private val dealerId = "dealer:8555"
  private val userId = "user:123"

  private val orderGen: Gen[Order] = {
    for {
      uuid <- Gen.uuid
      created <- Gen.oneOf(List(-200, -100, 0, 100, 200)).map(2000 + _).map(Timestamps.fromMillis(_))
      status <- Gen.oneOf(List(OrderStatus.SUCCESS, OrderStatus.UPDATING, OrderStatus.FAILED))
      user <- Gen.oneOf(List(dealerId, userId))
      reportType <- Gen.oneOf(
        OrdersApiModel.ReportType.GIBDD_REPORT,
        OrdersApiModel.ReportType.CM_EXPERT_REPORT,
        OrdersApiModel.ReportType.FULL_REPORT
      )
    } yield {
      Order.newBuilder
        .setOrderId(uuid.toString)
        .setCreated(created)
        .setStatus(status)
        .setUserId(user)
        .setReportType(reportType)
        .build
    }
  }

  private def buildOrder(): Order = {
    val uuid = UUID.randomUUID()
    val builder = Order.newBuilder().setOrderId(uuid.toString).setReportType(OrdersApiModel.ReportType.GIBDD_REPORT)
    builder.build()
  }

  test("waiting queue") {
    val notReadyIdsShard0 = Set(0)
    val notReadyIdsShard1 = Set(1)
    val readyIdsShard0 = Set(2, 3)
    val readyIdsShard1 = Set(4, 5, 6)

    def generateOrder(id: Int): OrdersTableRow = {
      val tsVisit = {
        if (notReadyIdsShard0.contains(id) || notReadyIdsShard1.contains(id)) {
          System.currentTimeMillis() + 1.hour.toMillis
        } else {
          System.currentTimeMillis() - 1.hour.toMillis
        }
      }
      val murmurHash3 = {
        if (notReadyIdsShard0.contains(id) || readyIdsShard0.contains(id)) 0 else 1
      }
      val order = buildOrder()

      OrdersTableRow.fromProtoOrder(order, tsVisit).copy(murMurHash = murmurHash3)
    }

    val orders: Map[Int, OrdersTableRow] = (0 until 7).map(id => id -> generateOrder(id)).toMap

    // insert elements to db
    orders.foreach(order => dao.insertRow(order._2).await)
    orders.foreach(order => {
      val dbOrder = dao.getRow(order._2.orderId, false).await
      assert(order._2 == dbOrder.get)
    })

    // check waiting queue size
    val waitingQueueShard0 = dao.getWaitingQueueSize(0, 2)
    val waitingQueueShard1 = dao.getWaitingQueueSize(1, 2)

    assert(waitingQueueShard0 == 2)
    assert(waitingQueueShard1 == 3)

    // check wating queue ids
    val candidatesIdsShard0 = dao.getCandidates(0, 2, 100)(_.map(_._1)).toList
    val candidatesIdsShard1 = dao.getCandidates(1, 2, 100)(_.map(_._1)).toList

    assert(candidatesIdsShard0.size == 2)
    readyIdsShard0.map(id => orders(id)).foreach(order => candidatesIdsShard0.contains(order.orderId))
    assert(candidatesIdsShard1.size == 3)
    readyIdsShard1.map(id => orders(id)).foreach(order => candidatesIdsShard1.contains(order.orderId))
  }

  test("crud") {
    val order = buildOrder()
    val uuid = order.getOrderId

    val optOrderBeforeInsert = dao.getOrder(uuid, false).await
    assert(optOrderBeforeInsert.isEmpty)

    dao.upsert(order, 123L).await

    val optOrderAfterInsert = dao.getOrder(uuid, onMaster = false).await
    assert(optOrderAfterInsert.nonEmpty)

    val updatedOrder = order.toBuilder.setCreated(Timestamps.fromMillis(123L)).build()
    dao.upsert(updatedOrder, 123L).await

    val optOrderAfterUpdate = dao.getOrder(uuid, onMaster = false).await
    assert(optOrderAfterUpdate.nonEmpty)
    assert(optOrderAfterUpdate.get == updatedOrder)

    val vinOrder = order.toBuilder.setOrderId(UUID.randomUUID().toString).setVin("Z8T4C5FS9BM005269").build()
    dao.upsert(vinOrder, 123L).await

    val filter = OrdersFilter.newBuilder().setVin("Z8T4C5FS9BM005269").build()
    val paging = Paging.Default
    val orders = dao.getOrders(filter, paging, onMaster = false).await
    assert(orders.length == 1)
    assert(orders.head == vinOrder)

    {
      // 1. Генерим заказы
      val orders = (0 until 50).map(_ => orderGen.sample.get)

      // 2. Фильтруем/сортируем их руками
      val ordersFilteredTrusted = orders
        .filter { o =>
          o.getUserId == dealerId &&
          List(OrderStatus.SUCCESS, OrderStatus.UPDATING).contains(o.getStatus) &&
          o.getCreated.toMillis >= 1800 &&
          o.getCreated.toMillis <= 2100
        }
        .sortBy(-_.getCreated.toMillis)
        .slice(5, 10)

      // 3. Вставляем в бд
      orders.foreach(dao.insert(_, 123).await)

      // 4. Фильтруем/сортируем в бд, с помощью dao
      val filter = OrdersFilter.newBuilder
        .setUserId(dealerId)
        .addAllOrderStatus(List(PublicOrderModel.Status.SUCCESS, PublicOrderModel.Status.UPDATING).asJava)
        .setCreatedFrom(Timestamps.fromMillis(1800))
        .setCreatedTo(Timestamps.fromMillis(2100))
        .build
      val paging = Paging(page = 2, pageSize = 5)
      val ordersFiltered = dao.getOrders(filter, paging, onMaster = false).await

      // 5. Сравниваем результат, полученный руками, с результатом, полученным из dao
      assert(ordersFiltered.size == ordersFilteredTrusted.size)
      ordersFiltered.zip(ordersFilteredTrusted).foreach { case (o, oTrusted) =>
        assert(o.getOrderId == oTrusted.getOrderId)
        assert(o.getUserId == oTrusted.getUserId)
        assert(o.getCreated == oTrusted.getCreated)
        assert(o.getStatus == oTrusted.getStatus)
      }
    }
  }

  test("lock and update") {
    val order = buildOrder()
    val uuid = order.getOrderId

    dao.insert(order, 123L).await

    val res = dao.lockAndUpdate(uuid) { holder =>
      val update = holder.toUpdate.withUpdate(order => {
        order.toBuilder.setCreated(Timestamps.fromMillis(123L)).build()
      })
      update -> update
    }

    val after = dao.getOrder(uuid, false).await

    assert(after.get == res.state)

  }

  test("lock and update on shard") {
    val order1 = OrdersTableRow.fromProtoOrder(buildOrder(), 1L).copy(murMurHash = 0)
    val order2 = OrdersTableRow.fromProtoOrder(buildOrder(), 1L).copy(murMurHash = 0)
    val order3 = OrdersTableRow.fromProtoOrder(buildOrder(), 1L).copy(murMurHash = 1)

    List(order1, order2, order3).foreach(row => dao.insertRow(row).await)

    val results = dao.lockAndUpdateOnShard(0, 2, List(order1.orderId, order2.orderId)) { holders =>
      holders.map(holder => {
        val update = holder.toUpdate.withUpdate(state => {
          val builder = state.toBuilder
          builder.getWatchingStateBuilder.setLastVisited(nowProto)
          builder.build()
        })

        (holder.id, update, update)
      })
    }

    val updatedOrder1 = dao.getRow(order1.orderId, false).await
    val updatedOrder2 = dao.getRow(order2.orderId, false).await
    val updatedOrder3 = dao.getRow(order3.orderId, false).await

    assert(updatedOrder1.get.order == results.find(_._1 == order1.orderId).get._2.state)
    assert(updatedOrder2.get.order == results.find(_._1 == order2.orderId).get._2.state)

    // из другого шарда
    assert(updatedOrder3.get == order3)
  }

}
