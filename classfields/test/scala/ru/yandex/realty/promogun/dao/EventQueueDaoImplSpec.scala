package ru.yandex.realty.promogun.dao

import org.junit.runner.RunWith
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.db.testcontainers.MySQLTestContainer
import ru.yandex.realty.doobie.Doobie._
import ru.yandex.realty.doobie.{DoobieTestDatabase, StubDbMonitorFactory}
import ru.yandex.realty.promogun.proto.scalapb.api.internal.PromoCode
import ru.yandex.realty.promogun.proto.scalapb.events.internal.{PromogunEvent, PromogunFireEvent}
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class EventQueueDaoImplSpec extends AsyncSpecBase with MySQLTestContainer.V8_0 with DoobieTestDatabase {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  implicit val t: Traced = Traced.empty
  private val dao = new EventQueueDaoImpl(new StubDbMonitorFactory)

  before {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/schema.sql")).futureValue
  }

  after {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/drop_tables.sql")).futureValue
  }

  def mkEvents(from: Int, to: Int): List[PromogunEvent] =
    (from until to).map { i =>
      PromogunFireEvent(
        promoCode = Some(PromoCode(promoCodeId = i.toString, code = s"ABCDEFG$i"))
      )
    }.toList

  "EventQueueDaoImplSpec" should {
    "one event" in {
      val event = PromogunFireEvent(
        promoCode = Some(PromoCode(promoCodeId = "314159", code = "ABCDEFG"))
      )

      transaction(dao.count).futureValue shouldBe 0
      transaction(dao.insert(event :: Nil)).futureValue
      transaction(dao.count).futureValue shouldBe 1
      val Seq(received) = transaction(dao.take(1)).futureValue
      received.event shouldBe event
      transaction(dao.count).futureValue shouldBe 1
      transaction(dao.remove(received.id :: Nil)).futureValue
      transaction(dao.count).futureValue shouldBe 0

    }
    "100 + 100 events" in {
      val events1 = mkEvents(1000, 1100)
      val events2 = mkEvents(1100, 1200)

      transaction {
        dao.insert(events1)
      }.futureValue
      transaction(dao.count).futureValue shouldBe 100
      transaction {
        dao.insert(events2)
      }.futureValue
      transaction(dao.count).futureValue shouldBe 200
      val received = transaction(dao.take(100)).futureValue
      received should have length (100)
      events1.foreach { e =>
        received.map(_.event) should contain(e)
      }
      transaction(dao.count).futureValue shouldBe 200
      transaction(dao.remove(received.map(_.id))).futureValue
      transaction(dao.count).futureValue shouldBe 100
    }

    "multiple take get different rows" in {
      val events1 = mkEvents(1000, 1100)
      transaction {
        dao.insert(events1)
      }.futureValue

      val (received1, received2) = transaction {
        for {
          r1 <- dao.take(40)
          r2 <- dao.take(40)
        } yield (r1, r2)
      }.futureValue

      received1 should have length (40)
      received2 should have length (40)

      (received1 ++ received2).filterNot { e =>
        received1.contains(e) && received2.contains(e)
      } shouldBe empty
    }

    "take less than limit" in {
      transaction(dao.take(100)).futureValue shouldBe empty
      val events = mkEvents(1000, 1050)
      transaction {
        dao.insert(events)
      }.futureValue
      transaction(dao.take(100)).futureValue should have length (50)
    }

    "keep insert order" in {
      val events = mkEvents(1000, 1100)
      transaction {
        events.traverse(e => dao.insert(e :: Nil))
      }.futureValue
      val received = transaction(dao.take(10)).futureValue

      transaction(dao.take(10)).futureValue shouldBe received

      transaction(dao.remove(received(5).id :: Nil)).futureValue
      val afterRemove = transaction(dao.take(9)).futureValue

      afterRemove shouldBe received.filter(_.id != received(5).id)
    }
  }
}
