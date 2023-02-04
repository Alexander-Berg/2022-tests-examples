package auto.dealers.booking.scheduler.events

import java.time.OffsetDateTime
import common.zio.events_broker.Broker
import ru.auto.booking.broker.broker_model.BookingChangeEvent
import ru.auto.booking.common_model.BookingStatus.PAID
import auto.dealers.booking.model.BookingEvent
import auto.dealers.booking.scheduler.events
import auto.dealers.booking.scheduler.notifications.NotificationsManager
import auto.dealers.booking.storage.BookingDao
import auto.dealers.booking.storage.memory.InMemoryBookingDao
import auto.dealers.booking.testkit.BookingModelGens._
import auto.dealers.booking.api.uuid.TestUUID
import zio._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, _}

import scala.collection.mutable

object PushBookingEventsTaskSpec extends DefaultRunnableSpec {

  val pushedEvents = mutable.Map.empty[Long, Int]
  val pushedNotifications = mutable.Map.empty[Long, Int]

  val testEventsBroker: Layer[Nothing, Has[Broker.Typed[BookingChangeEvent]]] =
    ZLayer.succeed(new Broker.Typed[BookingChangeEvent] {

      override def send(
          event: BookingChangeEvent,
          id: Option[String],
          schemaVersion: Option[String]): IO[Broker.BrokerError, Unit] = incrementCount(pushedEvents, event.id)
    })

  val testNotificationManager: Layer[Nothing, Has[NotificationsManager.Service]] =
    ZLayer.succeed(new NotificationsManager.Service {

      override def notifyOnStatusChange(event: BookingEvent): Task[Unit] =
        incrementCount(pushedNotifications, event.id)
    })

  def incrementCount(map: mutable.Map[Long, Int], id: Long): UIO[Unit] =
    ZIO.effectTotal(map.put(id, map.getOrElse(id, 0) + 1)).unit

  def dropCounts: URIO[Any, Unit] = {
    ZIO.effectTotal {
      pushedEvents.clear()
      pushedNotifications.clear()
    }.unit
  }

  val task = new events.PushBookingEventsTask

  override def spec: ZSpec[TestEnvironment, Any] = {

    suite("Push booking event task")(
      testM("push event without previous state") {
        for {
          created <- BookingDao.createBooking(createBooking)
          waitingForPush <- BookingDao.getWaitingForPush
          _ <- task.program
          eventsPushCountAfterInsert = pushedEvents.getOrElse(waitingForPush.head.id, 0)
          notificationPushCountAfterInsert = pushedNotifications.getOrElse(waitingForPush.head.id, 0)
          afterPush <- BookingDao.getWaitingForPush
        } yield assert(afterPush)(isEmpty) &&
          assert(waitingForPush)(hasSize(equalTo(1))) &&
          assert(waitingForPush.head.currentState)(equalTo(created)) &&
          assert(eventsPushCountAfterInsert)(equalTo(1)) &&
          assert(notificationPushCountAfterInsert)(equalTo(1)) &&
          assert(waitingForPush.head.previousState)(isNone)
      },
      testM("push event with previous state") {
        for {
          inserted <- BookingDao.createBooking(anotherBooking(createBooking))
          waitingAfterInsert <- BookingDao.getWaitingForPush
          _ <- task.program
          eventsPushCountAfterInsert = pushedEvents.getOrElse(waitingAfterInsert.head.id, 0)
          notificationPushCountAfterInsert = pushedNotifications.getOrElse(waitingAfterInsert.head.id, 0)
          waitingAfterPush <- BookingDao.getWaitingForPush
          _ <- BookingDao.savePaymentInfo(OffsetDateTime.now(), inserted.bookingTransactionId, "bankerTransactionId")
          withSavedPayment <- BookingDao.getBooking(inserted.bookingTransactionId)
          waitingAfterChangeState <- BookingDao.getWaitingForPush
          _ <- task.program
          eventsPushCountAfterPayment = pushedEvents.getOrElse(waitingAfterChangeState.head.id, 0)
          notificationPushCountAfterPayment = pushedNotifications.getOrElse(waitingAfterChangeState.head.id, 0)
          waitingAfterPush1 <- BookingDao.getWaitingForPush
        } yield assert(waitingAfterInsert)(hasSize(equalTo(1))) &&
          assert(waitingAfterInsert.head.previousState)(isNone) &&
          assert(waitingAfterInsert.head.currentState)(equalTo(inserted)) &&
          assert(eventsPushCountAfterInsert)(equalTo(1)) &&
          assert(notificationPushCountAfterInsert)(equalTo(1)) &&
          assert(waitingAfterPush)(isEmpty) &&
          assert(eventsPushCountAfterPayment)(equalTo(1)) &&
          assert(notificationPushCountAfterPayment)(equalTo(1)) &&
          assert(withSavedPayment.status)(equalTo(PAID)) &&
          assert(waitingAfterChangeState)(hasSize(equalTo(1))) &&
          assert(waitingAfterChangeState.head.currentState)(equalTo(withSavedPayment)) &&
          assert(waitingAfterChangeState.head.previousState)(equalTo(Some(inserted))) &&
          assert(waitingAfterPush1)(isEmpty)
      },
      testM("don't push already pushed events") {
        for {
          inserted <- BookingDao.createBooking(anotherBooking(createBooking))
          waitingAfterInsert <- BookingDao.getWaitingForPush
          _ <- task.program
          eventsPushCountAfterFirstAttempt = pushedEvents.getOrElse(waitingAfterInsert.head.id, 0)
          notificationPushCountAfterFirstAttempt = pushedNotifications.getOrElse(waitingAfterInsert.head.id, 0)
          _ <- task.program
          eventsPushCountAfterSecondAttempt = pushedEvents.getOrElse(waitingAfterInsert.head.id, 0)
          notificationPushCountAfterSecondAttempt = pushedNotifications.getOrElse(waitingAfterInsert.head.id, 0)
        } yield assert(waitingAfterInsert.head.currentState)(equalTo(inserted)) &&
          assert(waitingAfterInsert)(hasSize(equalTo(1))) &&
          assert(eventsPushCountAfterFirstAttempt)(equalTo(1)) &&
          assert(notificationPushCountAfterFirstAttempt)(equalTo(1)) &&
          assert(eventsPushCountAfterSecondAttempt)(equalTo(1)) &&
          assert(notificationPushCountAfterSecondAttempt)(equalTo(1))
      },
      testM("successfully push multiple events") {
        val booking1 = createBooking
        val booking2 = anotherBooking(booking1)
        val booking3 = anotherBooking(booking2)
        for {
          created1 <- BookingDao.createBooking(booking1)
          created2 <- BookingDao.createBooking(booking2)
          created3 <- BookingDao.createBooking(booking3)
          waiting <- BookingDao.getWaitingForPush
          _ <- task.program
          pushes <- ZIO.foreach(waiting) { event =>
            ZIO.succeed(
              (pushedEvents.getOrElse(event.id, 0), pushedNotifications.getOrElse(event.id, 0))
            )
          }
          waitingAfterPush <- BookingDao.getWaitingForPush
        } yield assert(waiting.map(_.currentState))(hasSameElements(Seq(created1, created2, created3))) &&
          assert(pushes)(forall(equalTo((1, 1)))) && assert(waitingAfterPush)(isEmpty)
      }
    ) @@ before(InMemoryBookingDao.clean) @@ before(dropCounts) @@ sequential
  }.provideCustomLayer(TestUUID.test ++ InMemoryBookingDao.live ++ testEventsBroker ++ testNotificationManager)
}
