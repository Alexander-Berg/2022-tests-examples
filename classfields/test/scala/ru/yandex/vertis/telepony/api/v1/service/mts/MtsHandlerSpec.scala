package ru.yandex.vertis.telepony.api.v1.service.mts

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.joda.time.DateTime
import org.mockito.Mockito.{times, verify}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.v1.MtsEventLog
import ru.yandex.vertis.telepony.api.{DomainExceptionHandler, DomainMarshalling, RequestDirectives, RouteTest}
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.json.{EventParser, InternalEvent, InternalEventProtocol}
import ru.yandex.vertis.telepony.model.Action.NoAction
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.EventReactionService

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author neron
  */
class MtsHandlerSpec extends RouteTest with DomainExceptionHandler with DomainMarshalling with MockitoSupport {

  implicit val am: ActorMaterializer = ActorMaterializer()(system)

  implicit val viewMarshaller: ToEntityMarshaller[InternalEvent] =
    sprayJsonMarshallerConverter(InternalEventProtocol.InternalEventFormat)

  trait Test {
    val mockedCallEventJournal = mock[WriteJournal[MtsCallEventAction]]

    val mockedEventReactionService = mock[EventReactionService]

    val route: Route = RequestDirectives.wrapRequest {
      RequestDirectives.seal(
        new MtsHandler {
          override def domain: TypedDomain = TypedDomains.billing_realty

          override val mtsEventLog: MtsEventLog = new MtsEventLog("boom")

          override def eventReactionService: EventReactionService = mockedEventReactionService

          override def callEventJournal: WriteJournal[MtsCallEventAction] = mockedCallEventJournal

          override def eventMapping: (Event) => Event = identity

          override def mtsTimeout: FiniteDuration = 100.millis
        }.route
      )
    }
  }

  private val event = InternalEvent(
    CallID = 1,
    EventTime = DateTime.now,
    AN = None,
    UN = "9312320032",
    DN1 = None,
    DN2 = None,
    EXT2 = None,
    Result = None,
    EventType = EventTypes.CallAccepted.id
  )

  private val modelEvent = EventParser.toEvent(event)

  "Handler" should {
    "handle and response with empty 200 ok" in new Test {
      when(mockedCallEventJournal.send(?)).thenReturn(Future.successful(null))
      when(mockedEventReactionService.react(modelEvent))
        .thenReturn(Future.successful(EventReaction(NoAction(modelEvent.externalId))))
      Post("/CallEvent", event) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe empty
        verify(mockedCallEventJournal, times(1)).send(?)
        verify(mockedEventReactionService, times(1)).react(modelEvent)
      }
    }
  }

}
