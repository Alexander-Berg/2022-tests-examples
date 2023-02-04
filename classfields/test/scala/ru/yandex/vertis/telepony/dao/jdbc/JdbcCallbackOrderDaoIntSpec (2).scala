package ru.yandex.vertis.telepony.dao.jdbc

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.jdbc.api._
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.CallbackGenerator._
import ru.yandex.vertis.telepony.model.CallbackOrder._
import ru.yandex.vertis.telepony.model.{ObjectId, Tag, TypedDomain, TypedDomains}
import ru.yandex.vertis.telepony.service.CallbackOrderService.{ByObjectId, ByStatusUpdateTime, ByStatuses, CallbackOrderExists, StatusFilter}
import ru.yandex.vertis.telepony.time._
import ru.yandex.vertis.telepony.util.CallFilters.TagFilter
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate
import ru.yandex.vertis.telepony.util.db.SlickDb._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success
import scala.annotation.nowarn

/**
  * @author neron
  */
class JdbcCallbackOrderDaoIntSpec extends SpecBase with BeforeAndAfterEach with JdbcSpecTemplate {

  private lazy val domain: TypedDomain = TypedDomains.autoru_def
  private lazy val dao = new JdbcCallbackOrderDao(dualDb)

  private val Limit = 100

  @nowarn
  override def beforeEach(): Unit = {
    dualDb.master.run("", JdbcCallbackOrderDao.CallbackOrderQuery.delete).futureValue
  }

  "CallbackOrderDao" should {
    "create new" in {
      val orderRequest = CallbackOrderRequestGen.next
      val createTime = DateTime.now()
      val callbackOrderSource = CallbackOrderSourceGen.next
      val order = dao.create(domain, orderRequest, callbackOrderSource, createTime).futureValue
      order.objectId shouldBe orderRequest.objectId
      order.tag shouldBe orderRequest.tag
      order.payload shouldBe orderRequest.payload
      order.source shouldBe orderRequest.source
      order.target shouldBe orderRequest.target
      order.status.value shouldBe Statuses.Scheduled
      order.status.updateTime shouldBe createTime
      order.createTime shouldBe createTime
    }

    "not create duplicate" in {
      val existing = CallbackOrderRequestGen.next
      val duplicate = {
        val x = CallbackOrderRequestGen.next
        x.copy(
          objectId = existing.objectId,
          tag = existing.tag,
          source = x.source.copy(number = existing.source.number),
          target = x.target.copy(number = existing.target.number)
        )
      }
      val callbackOrderSource = CallbackOrderSourceGen.next

      dao.create(domain, existing, callbackOrderSource, DateTime.now()).futureValue
      dao
        .create(domain, duplicate, callbackOrderSource, DateTime.now())
        .failed
        .futureValue shouldBe an[CallbackOrderExists]
    }

    "not create duplicate concurrently" in {
      val params = CallbackOrderRequestGen.next
      val createTime = DateTime.now()
      val duplicates = 1.to(10).map { _ =>
        val x = CallbackOrderRequestGen.next
        x.copy(
          objectId = params.objectId,
          tag = params.tag,
          source = x.source.copy(number = params.source.number),
          target = x.target.copy(number = params.target.number)
        )
      }
      val callbackOrderSource = CallbackOrderSourceGen.next

      import ru.yandex.vertis.telepony.util.Threads.lightWeightTasksEc

      val resultsF = duplicates
        .map(dao.create(domain, _, callbackOrderSource, createTime))
        .map(_.transform(t => Success(t)).map(_.toEither))
      val results = Future.sequence(resultsF).futureValue
      results.count(_.isRight) shouldBe 1
      results.collect {
        case Left(th) =>
          th shouldBe an[CallbackOrderExists]
      }
    }

    "change status" in {
      val orderRequest = CallbackOrderRequestGen.next
      val nextStatus = CallbackOrderStatusGen.next
      val callbackOrderSource = CallbackOrderSourceGen.next
      val order = dao.create(domain, orderRequest, callbackOrderSource).futureValue
      val isUpdated = dao.updateStatus(order.id, order.status.value, nextStatus).futureValue
      isUpdated shouldBe true
      val changed = dao.get(order.id).futureValue
      inside(changed) { case Some(newOrder) => newOrder.status shouldBe nextStatus }
    }

    "not change status if expected status is wrong" in {
      val orderRequest = CallbackOrderRequestGen.next
      val nextStatus = CallbackOrderStatusGen.next
      val expectedStatusValue = CallbackOrderStatusValueGen.suchThat(_ != Statuses.Scheduled).next
      val callbackOrderSource = CallbackOrderSourceGen.next
      val order = dao.create(domain, orderRequest, callbackOrderSource).futureValue
      val isUpdated = dao.updateStatus(order.id, expectedStatusValue, nextStatus).futureValue
      isUpdated shouldBe false
      val changed = dao.get(order.id).futureValue
      inside(changed) { case Some(newOrder) => newOrder shouldBe order }
    }

    "allow to create new order if previous is completed or cancelled" in {
      val completedRequest = CallbackOrderRequestGen.next
      val callbackOrderSource = CallbackOrderSourceGen.next
      val completed = dao.create(domain, completedRequest, callbackOrderSource).futureValue
      val newOrder = {
        val x = CallbackOrderRequestGen.next
        x.copy(
          objectId = completed.objectId,
          tag = completed.tag,
          source = x.source.copy(number = completed.source.number)
        )
      }

      dao.create(domain, newOrder, callbackOrderSource).futureValue
    }

    "filter statuses" in {
      val order1Req = CallbackOrderRequestGen.next
      val order2Req = CallbackOrderRequestGen.next
      val order2Status = CallbackOrderStatus(Statuses.Processing, DateTime.now().minus(2.hours))
      val order3Req = CallbackOrderRequestGen.next
      val order3Status = CallbackOrderStatus(Statuses.Cancelled, DateTime.now().minus(1.hours))
      val callbackOrderSource = CallbackOrderSourceGen.next
      val Seq(order1, order2, order3) =
        Seq(order1Req, order2Req, order3Req).map(dao.create(domain, _, callbackOrderSource).futureValue)
      dao.updateStatus(order2.id, Statuses.Scheduled, order2Status).futureValue
      dao.updateStatus(order3.id, Statuses.Scheduled, order3Status).futureValue

      val orders1 = dao.list(ByStatuses(Set(Statuses.Processing)), Limit).futureValue
      val orders2 = dao.list(ByStatuses(Set(Statuses.Processing, Statuses.Cancelled)), Limit).futureValue

      orders1.map(_.status.value) shouldBe Seq(Statuses.Processing)
      orders2.map(_.status.value) shouldBe Seq(Statuses.Processing, Statuses.Cancelled)
    }

    "get by orderId" in {
      val orderRequest = CallbackOrderRequestGen.next
      val callbackOrderSource = CallbackOrderSourceGen.next
      val order = dao.create(domain, orderRequest, callbackOrderSource).futureValue
      val actualOrderOpt = dao.get(order.id).futureValue
      inside(actualOrderOpt) { case Some(actualOrder) => actualOrder shouldBe order }
    }

    "filter object id and tag" in {
      val status1 = CallbackOrderStatus(Statuses.Processing, DateTimeGen.next)
      val status2 = CallbackOrderStatus(Statuses.Failed, DateTimeGen.next)

      Seq(
        (ObjectId("id1"), Tag.Empty, status1),
        (ObjectId("id1"), Tag.NonEmpty("tag"), status1),
        (ObjectId("id1"), Tag.NonEmpty("tag1"), status2),
        (ObjectId("id2"), Tag.Empty, status1),
        (ObjectId("id2"), Tag.NonEmpty("tag"), status1),
        (ObjectId("id2"), Tag.NonEmpty("tag1"), status2)
      ).zipWithIndex.foreach {
        case ((objectId, tag, status), idx) =>
          val orderRequest = CallbackOrderRequestGen.next.copy(objectId = objectId, tag = tag)
          val callbackOrderSource = CallbackOrderSourceGen.next
          val order =
            dao.create(domain, orderRequest, callbackOrderSource, DateTime.now().plus(idx.minutes)).futureValue
          dao.updateStatus(order.id, Statuses.Scheduled, status).futureValue
      }

      val orders1 =
        dao
          .list(ByObjectId(domain, ObjectId("id1"), Some(TagFilter.Full(Tag.Empty))), Limit)
          .futureValue
          .map(x => (x.objectId.value, x.tag, x.status.value))
      val orders2 =
        dao
          .list(ByObjectId(domain, ObjectId("id1"), Some(TagFilter.Full(Tag.NonEmpty("tag")))), Limit)
          .futureValue
          .map(x => (x.objectId.value, x.tag, x.status.value))
      val orders3 =
        dao
          .list(ByObjectId(domain, ObjectId("id1")), Limit)
          .futureValue
          .map(x => (x.objectId.value, x.tag, x.status.value))
      val orders4 =
        dao
          .list(ByObjectId(domain, ObjectId("id2"), statusFilter = Some(StatusFilter(Set(Statuses.Processing)))), Limit)
          .futureValue
          .map(x => (x.objectId.value, x.tag, x.status.value))

      orders1 shouldBe Seq(("id1", Tag.Empty, Statuses.Processing))
      orders2 shouldBe Seq(("id1", Tag.NonEmpty("tag"), Statuses.Processing))
      val res =
        Seq(
          ("id1", Tag.Empty, Statuses.Processing),
          ("id1", Tag.NonEmpty("tag"), Statuses.Processing),
          ("id1", Tag.NonEmpty("tag1"), Statuses.Failed)
        )
      orders3 should contain theSameElementsAs res
      orders4 shouldBe Seq(("id2", Tag.Empty, Statuses.Processing), ("id2", Tag.NonEmpty("tag"), Statuses.Processing))
    }

    "filter by status update time with sorting by time" in {
      val beforeFrom = DateTimeGen.next
      val from = beforeFrom.plus(DurationHoursGen.next)
      val between1 = from.plus(DurationHoursGen.next)
      val between2 = between1.plus(DurationHoursGen.next)
      val to = between2.plus(DurationHoursGen.next)
      val afterTo = to.plus(DurationHoursGen.next)

      val Seq(_, _, _, order1, order2, _, _) = Seq(
        CallbackOrderStatus(Statuses.Scheduled, beforeFrom),
        CallbackOrderStatus(Statuses.Failed, beforeFrom),
        CallbackOrderStatus(Statuses.Scheduled, between1),
        CallbackOrderStatus(Statuses.Cancelled, between1),
        CallbackOrderStatus(Statuses.Completed, between2),
        CallbackOrderStatus(Statuses.Processing, afterTo),
        CallbackOrderStatus(Statuses.Completed, afterTo)
      ).map { status =>
        val orderRequest = CallbackOrderRequestGen.next
        val callbackOrderSource = CallbackOrderSourceGen.next
        val order = dao.create(domain, orderRequest, callbackOrderSource, DateTime.now()).futureValue
        dao.updateStatus(order.id, Statuses.Scheduled, status).futureValue
        dao.get(order.id).futureValue.get
      }

      val actualOrders = dao.list(ByStatusUpdateTime(from, to, TerminatedStatuses), Limit).futureValue

      actualOrders shouldBe Seq(order1, order2)
    }

  }

}
