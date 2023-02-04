package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.verify
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.generator.BeelineGenerator._
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.RouteResults
import ru.yandex.vertis.telepony.model.beeline.{BeelineCallEvent, CallRoutedEvent, RoutingResponse}
import ru.yandex.vertis.telepony.service.CallRoutingService
import ru.yandex.vertis.telepony.service.CallRoutingService.Destination
import ru.yandex.vertis.telepony.util.Threads.lightWeightTasksEc

import scala.concurrent.Future

/**
  * @author neron
  */
class BeelineReactionServiceSpec extends SpecBase {

  private trait Test extends MockitoSupport {
    val domain = DomainGen.next
    val callSettings = CallSettingsGen.next
    val mockedCallRouting = mock[CallRoutingService]
    val mockedJournal = mock[WriteJournal[BeelineCallEvent]]
    val service = new BeelineReactionServiceImpl(mockedCallRouting, callSettings, mockedJournal, domain)
  }

  private class ReactTestImpl(emptySource: Boolean = false) extends Test {

    val request = if (emptySource) {
      RoutingRequestGen.next.copy(callerNumber = None)
    } else {
      RoutingRequestGen.next
    }
    val target = PhoneGen.next
    val routeResult = RouteResults.Passed
    val destination = Destination(target, routeResult)
    when(mockedCallRouting.route(equ(request.refinedSource), equ(request.operatorNumber)))
      .thenReturn(Future.successful(destination))

    val expectedResponse = RoutingResponse.from(
      request = request,
      target = target,
      settings = callSettings,
      domainOpt = Some(domain)
    )

    val expectedCallRoutedEvent = BeelineReactionServiceImpl.routedEvent(
      request = request,
      response = expectedResponse,
      routeResult = routeResult,
      time = DateTime.now()
    )

    def eventMatcher = ArgumentMatchers.argThat[CallRoutedEvent] { e =>
      e.copy(time = expectedCallRoutedEvent.time) == expectedCallRoutedEvent
    }
    when(mockedJournal.send(eventMatcher)).thenReturn(Future.successful(null))
    val response = service.react(request).futureValue
    response should ===(expectedResponse)

    verify(mockedCallRouting).route(equ(request.refinedSource), equ(request.operatorNumber))
    verify(mockedJournal).send(eventMatcher)
  }

  "BeelineReactionService" should {
    "react to RoutingRequest" in new ReactTestImpl()

    "react to RoutingRequest with empty caller" in new ReactTestImpl(emptySource = true)
  }

}
