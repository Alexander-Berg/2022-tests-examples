package ru.yandex.realty.rent.payments.dao

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.db.mysql.DuplicateRecordException
import ru.yandex.realty.rent.payments.DaoEventInitialization

@RunWith(classOf[JUnitRunner])
class EventDaoSpec extends DaoEventInitialization with WordSpecLike with ScalaFutures with Matchers {

  "EventDao" should {
    "append and find event" in {
      val event = eventGen().next
      doobieDatabase.masterTransaction {
        eventDao.append(Seq(event))(_)
      }.futureValue
      val eventByOrderId = doobieDatabase.masterTransaction {
        eventDao.findByOrderId(event.orderId, forUpdate = true)(_)
      }.futureValue
      val eventByEntityId = doobieDatabase.masterTransaction {
        eventDao.findByEntityId(event.entityId)(_)
      }.futureValue
      eventByOrderId.size shouldBe 1
      eventByOrderId.headOption shouldBe Some(event)
      eventByEntityId.size shouldBe 1
      eventByEntityId.headOption shouldBe Some(event)
    }

    "return empty seq if the event does not exist" in {
      val event = eventGen().next
      val eventByOrderId = doobieDatabase.masterTransaction {
        eventDao.findByOrderId(event.orderId, forUpdate = true)(_)
      }.futureValue
      val eventByEntityId = doobieDatabase.masterTransaction {
        eventDao.findByEntityId(event.entityId)(_)
      }.futureValue
      eventByOrderId.size shouldBe 0
      eventByEntityId.size shouldBe 0
    }

    "throw exception if the inserted order with id and idempotency_key already exists" in {
      val event = eventGen().next
      doobieDatabase.masterTransaction {
        eventDao.append(Seq(event))(_)
      }.futureValue

      val eventWithSameIdempotencyKey = eventGen(parent = Some(event.idempotencyKey)).next.copy(orderId = event.orderId)
      doobieDatabase.masterTransaction {
        eventDao.append(Seq(eventWithSameIdempotencyKey))(_)
      }.futureValue

      val duplicateEvent = eventGen(parent = Some(eventWithSameIdempotencyKey.idempotencyKey)).next
        .copy(orderId = event.orderId, idempotencyKey = event.idempotencyKey)
      doobieDatabase
        .masterTransaction {
          eventDao.append(Seq(duplicateEvent))(_)
        }
        .failed
        .futureValue shouldBe a[DuplicateRecordException]
    }

    "list all events by entity" in {
      val event1 = eventGen().next
      val event2 = eventGen().next.copy(parent = Some(event1.idempotencyKey), entityId = event1.entityId)
      val event3 = eventGen().next.copy(parent = Some(event2.idempotencyKey))
      doobieDatabase.masterTransaction(eventDao.append(Seq(event1, event2, event3))(_)).futureValue
      val events = doobieDatabase.masterTransaction {
        eventDao.findByEntityId(event1.entityId)(_)
      }.futureValue
      events.toSet shouldBe Set(event1, event2)
    }

    "list all events by order" in {
      val event1 = eventGen().next
      val event2 = eventGen().next.copy(parent = Some(event1.idempotencyKey), orderId = event1.orderId)
      val event3 = eventGen().next.copy(parent = Some(event2.idempotencyKey))
      doobieDatabase.masterTransaction(eventDao.append(Seq(event1, event2, event3))(_)).futureValue
      val events = doobieDatabase.masterTransaction {
        eventDao.findByOrderId(event1.orderId, forUpdate = true)(_)
      }.futureValue
      events.toSet shouldBe Set(event1, event2)
    }
  }
}
