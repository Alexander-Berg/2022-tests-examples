package ru.yandex.vertis.telepony.service.impl

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.verify
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.MttGenerator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.RouteResults
import ru.yandex.vertis.telepony.model.mtt.{CallRoutedEvent, MttCallEvent, RoutingResponse}
import ru.yandex.vertis.telepony.service.CallRoutingService
import ru.yandex.vertis.telepony.service.CallRoutingService.Destination
import ru.yandex.vertis.telepony.settings.MttPreMediaSettings
import ru.yandex.vertis.telepony.util.Threads.lightWeightTasksEc

import scala.concurrent.Future

/**
  * @author neron
  */
class MttReactionServiceSpec extends SpecBase {

  trait Test extends MockitoSupport {
    val richRoutingRequest = RichRoutingRequestGen.suchThat(_.sharedNumber.domain.isDefined).next
    val domain = richRoutingRequest.sharedNumber.domain.get
    val callSettings = CallSettingsGen.next
    val mockedCallRouting = mock[CallRoutingService]
    val eventUrl = "eventUrl"
    val mockedJournal = mock[WriteJournal[MttCallEvent]]

    val premediaSettings = MttPreMediaSettings(ConfigFactory.parseString(s"""
                                                                            | {
                                                                            |   files-to-sync = ["file"]
                                                                            |   ${callSettings.targetAudio.getOrElse(
                                                                              "target-audio"
                                                                            )} = targetAudioId
                                                                            |   ${callSettings.callerAudio.getOrElse(
                                                                              "caller-audio"
                                                                            )} = callerAudioId
                                                                            | }
                                                                            |""".stripMargin))

    val accountPremediaSettings = Map(richRoutingRequest.sharedNumber.account -> premediaSettings)

    val service = new MttReactionServiceImpl(
      mockedCallRouting,
      callSettings,
      eventUrl,
      mockedJournal,
      accountPremediaSettings,
      domain
    )
  }

  "MttReactionService" should {

    "react on RichRoutingRequest" in new Test {
      import richRoutingRequest._
      val target = PhoneGen.next
      val routeResult = RouteResults.Passed
      val destination = Destination(target, routeResult)
      when(mockedCallRouting.route(equ(Some(request.callerNumber)), equ(request.operatorNumber)))
        .thenReturn(Future.successful(destination))
      val expectedResponse = RoutingResponse(
        targetNumber = target,
        statusEventUrl = eventUrl,
        callerAudio = callSettings.callerAudio.map(_ => "callerAudioId"),
        targetAudio = callSettings.targetAudio.map(_ => "targetAudioId"),
        domain = Some(domain)
      )
      val expectedCallRoutedEvent = MttReactionServiceImpl.routedEvent(
        request = request,
        response = expectedResponse,
        routeResult = routeResult,
        time = DateTime.now()
      )
      def eventMatcher = ArgumentMatchers.argThat[CallRoutedEvent] { e =>
        e.copy(time = expectedCallRoutedEvent.time) == expectedCallRoutedEvent
      }
      when(mockedJournal.send(eventMatcher)).thenReturn(Future.successful(null))

      val response = service.react(richRoutingRequest).futureValue
      response should ===(expectedResponse)

      verify(mockedCallRouting).route(equ(Some(request.callerNumber)), equ(request.operatorNumber))
      verify(mockedJournal).send(eventMatcher)
    }

  }

}
