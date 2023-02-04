package ru.yandex.vertis.billing.dao

import billing.CommonModel
import billing.emon.Model.EventTypeNamespace.EventType
import billing.emon.Model.{Event, EventId, EventState}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.gens._
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.util.EmonUtils.RichEventState
import ru.yandex.vertis.billing.util.clean.CleanableDao

import scala.util.{Failure, Success, Try}

/**
  * @author rmuzhikov
  */
trait EmonEventDaoSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  protected def emonEventDao: EmonEventDao with CleanableDao

  override def beforeEach(): Unit = {
    emonEventDao.clean().get
    super.beforeEach()
  }

  "Emon event dao" should {

    "get empty event list when no events with search ids" in {
      val allEvents = EmonEventGen.next(201).toList
      emonEventDao.getEvents(allEvents.map(e => e.event.eventStateId)) match {
        case Success(events) =>
          events shouldBe Seq.empty
        case Failure(exception) =>
          throw exception
      }
    }

    "add events" in {
      val allEvents = EmonEventGen.next(201).toList
      (emonEventDao.insert(allEvents) should be).a(Symbol("Success"))
    }

    "get events by ids" in {
      val allEvents = EmonEventGen.next(201).toList
      (for {
        _ <- emonEventDao.insert(allEvents)
        events <- emonEventDao.getEvents(allEvents.map(e => e.event.eventStateId))
      } yield events) match {
        case Success(events) =>
          events.toSet shouldBe allEvents.toSet
        case Failure(exception) =>
          throw exception
      }
    }

    "ignore event updates" in {
      val events = EmonEventGen.next(10).toList
      (for {
        _ <- emonEventDao.insert(events)
        events1 <- emonEventDao.getEvents(events.map(e => e.event.eventStateId))
        _ <- emonEventDao.insert(events.map(e => e.copy(event = e.event.toBuilder.clearPrice().build())))
        events2 <- emonEventDao.getEvents(events.map(e => e.event.eventStateId))
      } yield (events1, events2)) match {
        case Success((events1, events2)) =>
          events1.toSet shouldBe events2.toSet
        case Failure(exception) =>
          throw exception
      }
    }

    "delete groups by ids" in {
      val allEvents = EmonEventGen.next(201).toList
      (for {
        _ <- emonEventDao.insert(allEvents)
        _ <- emonEventDao.deleteGroups(allEvents.map(_.groupId))
        events <- emonEventDao.getEvents(allEvents.map(e => e.event.eventStateId))
      } yield events) match {
        case Success(events) =>
          events shouldBe Seq.empty
        case Failure(exception) =>
          throw exception
      }
    }

    "get groups older than" in {
      (for {
        events1 <- Try(EmonEventGen.next(10).toSeq)
        events2 <- Try(EmonEventGen.next(10).toSeq)
        _ <- emonEventDao.insert(events1.map(_.copy(epoch = 1000)))
        _ <- emonEventDao.insert(events2.map(_.copy(epoch = 3000)))
        groups <- emonEventDao.getGroupsOlderThan(2000, events1.size + events2.size)
        _ <- emonEventDao.deleteGroups(events1.map(_.groupId) ++ events2.map(_.groupId))
      } yield (events1, groups)) match {
        case Success((events1, groups)) =>
          groups.toSet shouldBe events1.map(_.groupId).toSet
        case Failure(exception) =>
          throw exception
      }
    }

    val eventId1 = EventId
      .newBuilder()
      .setId("1")
      .setEventType(EventType.REALTY_DEVCHAT)
      .setProject(CommonModel.Project.REALTY)
      .build()

    val event1 =
      EmonEventDao.Event(
        EventState.newBuilder().setEvent(Event.newBuilder().setEventId(eventId1).build()).setSnapshotId(0).build(),
        "group1"
      )

    val eventId2 = EventId
      .newBuilder()
      .setId("2")
      .setEventType(EventType.REALTY_DEVCHAT)
      .setProject(CommonModel.Project.REALTY)
      .build()
    val event2 =
      EmonEventDao.Event(
        EventState.newBuilder().setEvent(Event.newBuilder().setEventId(eventId2).build()).setSnapshotId(1).build(),
        "group1",
        System.currentTimeMillis(),
        Some("tx2")
      )

    val eventId3 = EventId
      .newBuilder()
      .setId("3")
      .setEventType(EventType.REALTY_DEVCHAT)
      .setProject(CommonModel.Project.REALTY)
      .build()
    val event3 =
      EmonEventDao.Event(
        EventState.newBuilder().setEvent(Event.newBuilder().setEventId(eventId3).build()).setSnapshotId(0).build(),
        "group2"
      )
    val event4 =
      EmonEventDao.Event(
        EventState.newBuilder().setEvent(Event.newBuilder().setEventId(eventId3).build()).setSnapshotId(2).build(),
        "group2"
      )

    val testGroupEvents = Seq(event1, event2, event3, event4)

    "get last group events" in {
      (for {
        _ <- emonEventDao.insert(testGroupEvents)
        events1 <- emonEventDao.getEventsState("group1")
        events2 <- emonEventDao.getEventsState("group2")
      } yield (events1, events2)) match {
        case Success((events1, events2)) =>
          events1.toSet shouldBe Set(event1, event2)
          events2.toSet shouldBe Set(event4)
        case Failure(exception) =>
          throw exception
      }
    }

    "get groups without transaction" in {
      (for {
        _ <- emonEventDao.insert(testGroupEvents)
        groups <- emonEventDao.getGroupsWithoutTransaction(10)
      } yield groups) match {
        case Success(groups) =>
          groups.sorted shouldBe Seq("group1", "group2")
        case Failure(exception) =>
          throw exception
      }
    }

    "set transaction to old event" in {
      (for {
        _ <- emonEventDao.insert(testGroupEvents)
        _ <- emonEventDao.setTransaction(Seq(event3.event.eventStateId), "tx3")
        events <- emonEventDao.getEvents(Seq(event3.event.eventStateId))
      } yield events) match {
        case Success(events) =>
          events shouldBe Seq(event3.copy(transactionId = Some("tx3")))
        case Failure(exception) =>
          throw exception
      }
    }

    "get same groups without transaction after transaction set on old event" in {
      (for {
        _ <- emonEventDao.insert(testGroupEvents)
        _ <- emonEventDao.setTransaction(Seq(event3.event.eventStateId), "tx3")
        groups <- emonEventDao.getGroupsWithoutTransaction(testGroupEvents.size)
      } yield groups) match {
        case Success(groups) =>
          groups.sorted shouldBe Seq("group1", "group2")
        case Failure(exception) =>
          throw exception
      }
    }

    "get updated groups without transaction after transaction set on last event" in {
      (for {
        _ <- emonEventDao.insert(testGroupEvents)
        _ <- emonEventDao.setTransaction(Seq(event4.event.eventStateId), "tx4")
        groups <- emonEventDao.getGroupsWithoutTransaction(testGroupEvents.size)
      } yield groups) match {
        case Success(groups) =>
          groups.sorted shouldBe Seq("group1")
        case Failure(exception) =>
          throw exception
      }
    }

  }

}
