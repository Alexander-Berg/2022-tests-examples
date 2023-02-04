package ru.yandex.vertis.telepony.api.v1.service.vox

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.api.v1.MtsEventLog
import ru.yandex.vertis.telepony.api.{DomainExceptionHandler, DomainMarshalling, RequestDirectives, RouteTest}
import ru.yandex.vertis.telepony.generator.{Generator => ModelGenerator}
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.json.ReactiveJsonProtocol._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.model.vox.{FallbackData, FallbackResult, PhoneSettings}
import ru.yandex.vertis.telepony.service.{EventReactionService, FallbackDecider}

import scala.concurrent.Future

class VoxHandlerSpec
  extends RouteTest
  with DomainExceptionHandler
  with DomainMarshalling
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with SprayJsonSupport {

  trait Test {

    val reactionService = mock[EventReactionService]

    val eventJournal = mock[WriteJournal[VoxCallEventAction]]

    val fallbackDeciderMock = mock[FallbackDecider]

    val fallbackData = FallbackData(
      objectId = ObjectId("object-id"),
      fallbackPhone = Phone("+79817757575"),
      ivrAudioFile = Some("audio-file-uri"),
      moreInfo = Map("foo" -> "bar")
    )

    val route = RequestDirectives.wrapRequest {
      RequestDirectives.seal(
        new VoxHandler {

          override def eventReactionService = reactionService

          override def fallbackDecider: FallbackDecider = fallbackDeciderMock

          override def callEventJournal: WriteJournal[VoxCallEventAction] = eventJournal

          override def domain = TypedDomains.autoru_def

          override val mtsEventLog: MtsEventLog = new MtsEventLog("test")
        }.route
      )
    }
  }

  "VoxHandler" should {
    "response action for event" in new Test {
      forAll(EventModelGenerators.VoxEventsGen, Generators.EventReactionGen) { (event, reaction) =>
        when(reactionService.react(equ(event))).thenReturn(Future.successful(reaction))
        when(eventJournal.send(?)).thenReturn(Future.successful(null))
        Post("/event", event) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Action] shouldBe reaction.action
        }
      }
    }

    "return phone settings" in new Test {
      forAll(ModelGenerator.PhoneGen) { phone =>
        Get(s"/phones/${phone.value}/settings") ~> route ~> check {
          status shouldBe StatusCodes.OK
          //it is hardcoded atm
          responseAs[PhoneSettings] shouldBe PhoneSettings(recordEnabled = true)
        }
      }
    }

    "return fallback info" in new Test {
      forAll(ModelGenerator.PhoneGen, ModelGenerator.RefinedSourceGen) { (proxy, source) =>
        val fallbackResult = FallbackResult(needFallback = true, Some(fallbackData))
        when(fallbackDeciderMock.decide(?, ?)).thenReturn(Future.successful(fallbackResult))
        Get(s"/phones/${proxy.value}/sources/${source.callerId.value}/fallback_info") ~> route ~> check {
          status shouldBe StatusCodes.OK
          //it is hardcoded atm
          responseAs[FallbackResult] shouldBe fallbackResult
        }
      }
    }

  }

}
