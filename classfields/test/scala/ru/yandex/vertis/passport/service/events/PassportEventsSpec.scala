package ru.yandex.vertis.passport.service.events

import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterEach, WordSpec}
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.Domains
import ru.yandex.vertis.passport.dao.impl.mysql.MysqlEventsDao
import ru.yandex.vertis.passport.model.proto.EventPayload
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.proto.{ApiProtoFormats, EventsProtoFormats}
import ru.yandex.vertis.passport.service.events.EventsService.Filter
import ru.yandex.vertis.passport.service.log.PassportEventLog
import ru.yandex.vertis.passport.test.ModelGenerators.bool
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import ru.yandex.vertis.passport.util.DateTimeUtils._
import ru.yandex.vertis.passport.util.Page
import ru.yandex.vertis.passport.util.img.{ImageResolver, TestImageResolver}
import ru.yandex.vertis.passport.util.mysql.DualDatabase
import ru.yandex.vertis.tracing.Traced
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PassportEventsSpec
  extends WordSpec
  with SpecBase
  with BeforeAndAfterEach
  with GeneratorDrivenPropertyChecks
  with MySqlSupport
  with MockitoSupport {

  implicit val protoFormats = new ApiProtoFormats with EventsProtoFormats {
    override def imageResolver: ImageResolver = TestImageResolver
    override def environment: Environments.Value = Environments.Local
  }

  val domain = Domains.Auto

  val eventsDao = new MysqlEventsDao(domain, DualDatabase(dbs.passport))

  val brokerClient = mock[BrokerClient]
  when(brokerClient.send(?)(?)).thenReturn(Future.unit)

  val eventsService = new EventsServiceImpl(eventsDao)

  val eventLog = new PassportEventLog(domain, eventsService, brokerClient)

  val eventEnvelope = ModelGenerators.eventEnvelope(domain)

  // we don't differentiate between Api and Automated
  implicit override val requestContext: RequestContext =
    RequestContext(ApiPayload("test"), Traced.empty)

  override def afterEach() = {
    val f = {
      dbs.passport.underlying.run(sqlu"TRUNCATE events_log")
    }
    f.futureValue
  }

  "PassportEventLog" should {
    "save events" in {
      forAll(ModelGenerators.event) { event =>
        eventLog.logEvent(event).futureValue
      }
    }
  }

  "EventsService" should {
    "save and get events" in {
      val events = ModelGenerators.event
        .filter {
          case _: UserWasSeen => false
          case _ => true
        }
        .next(50)
      Future.sequence {
        events.map(eventLog.logEvent)
      }.futureValue

      val got = eventsService
        .list(Filter(), Page(0, 100))
        .futureValue

      got.events.map(_.event) should contain theSameElementsAs events
      got.totalCount shouldBe 50
    }

    "apply paging" in {
      val events = eventEnvelope.next(10)
      Future.sequence {
        events.map(eventsService.save)
      }.futureValue

      val got = eventsService
        .list(Filter(), Page(0, 3))
        .futureValue

      got.events.size shouldBe 3
      got.totalCount shouldBe 10
    }

    "apply DateTime interval filters" in {
      val events = eventEnvelope.next(10).toVector.sortBy(_.timestamp)
      Future.sequence {
        events.map(eventsService.save)
      }.futureValue

      val (begin, end0) = events.splitAt(3)
      val (expected, end) = end0.splitAt(3)

      val from = expected.head.timestamp
      val to = end.head.timestamp

      val got = eventsService
        .list(Filter(from = Some(from), to = Some(to)), Page(0, 10))
        .futureValue

      got.events.size shouldBe 3
      got.events should contain theSameElementsAs expected
    }

    "apply eventType filters" in {
      val events = eventEnvelope.values.takeWhile {
        case EventEnvelope(_, _, _: SmsSent, _) => false
        case _ => true
      }.toList ++ eventEnvelope.next(10)
      Future.sequence {
        events.map(eventsService.save)
      }.futureValue

      val smsSentEvents = events.collect {
        case e @ EventEnvelope(_, _, _: SmsSent, _) => e
      }

      val got = eventsService
        .list(Filter(eventType = Some(EventPayload.ImplCase.SMS_SENT)), Page(0, 1000))
        .futureValue

      got.events should contain theSameElementsAs smsSentEvents
    }

    "apply user ID and interval filters with paging" in {
      val count = 100
      val dates = ModelGenerators.dateInPast.next(10).toList.sorted
      val userIds = Gen.oneOf("1", "2", "3")
      val sessionId = ModelGenerators.fakeSessionId
      val events =
        for (ts <- dates)
          yield EventEnvelope(
            `domain`,
            ts,
            UserWasSeen(userIds.next, sessionId.next, bool.next),
            requestContext
          )
      Future.sequence {
        events.map(eventsService.save)
      }.futureValue

      val (_, end0) = events.splitAt(3)
      val (middle, end) = end0.splitAt(3)

      val from = middle.head.timestamp
      val to = end.head.timestamp

      val user1Events = middle.filter {
        case EventEnvelope(_, _, ue: UserEvent, _) if ue.userId == "1" => true
        case _ => false
      }

      val got = eventsService
        .list(
          Filter(
            userId = Some("1"),
            from = Some(from),
            to = Some(to)
          ),
          Page(0, 100)
        )
        .futureValue

      got.events should contain theSameElementsAs user1Events
    }
  }
}
