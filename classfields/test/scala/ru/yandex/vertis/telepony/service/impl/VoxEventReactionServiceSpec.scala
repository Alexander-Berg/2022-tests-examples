package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Action.{BlockAction, NoAction, RedirectAction, RouteNoRedirectAction}
import ru.yandex.vertis.telepony.model.Event.{CallAcceptedEvent, CallAnsweredEvent}
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.CallRoutingService
import ru.yandex.vertis.telepony.service.CallRoutingService.Destination
import ru.yandex.vertis.telepony.service.logging.LoggingReactionService
import ru.yandex.vertis.telepony.util.random.IdUtil

import scala.concurrent.Future

/**
  * @author neron
  */
class VoxEventReactionServiceSpec extends SpecBase {

  private def getCallAcceptedEvent(proxy: Phone): CallAcceptedEvent =
    CallAcceptedEvent(
      externalId = IdUtil.generateId64(),
      eventTime = DateTime.now(),
      sourceNumber = Some(RefinedSourceGen.next),
      proxyNumber = proxy
    )

  private def genCallAnsweredEvent(proxy: Phone): CallAnsweredEvent =
    CallAnsweredEvent(
      externalId = IdUtil.generateId64(),
      eventTime = DateTime.now(),
      sourceNumber = Some(RefinedSourceGen.next),
      proxyNumber = proxy
    )

  trait Test extends MockitoSupport {
    val targetPhone = PhoneGen.next
    val redirect = ActualRedirectGen.next
    val operatorPhone = redirect.source.number
    val callAcceptedEvent = getCallAcceptedEvent(operatorPhone)
    val callAnsweredEvent = genCallAnsweredEvent(operatorPhone)
    val caller = callAcceptedEvent.sourceNumber

    def destination(r: RouteResults.RouteResult) = Destination(targetPhone, r)

    val callRoutingService = mock[CallRoutingService]

    val eventReactionService =
      new VoxEventReactionServiceImpl(callRoutingService) with LoggingReactionService[Event, EventReaction] {
        override protected def operator: Operator = Operators.Vox
      }
  }

  "event reaction service" should {
    "redirect callAcceptedEvent with Block resolution to special targets" in new Test {
      when(callRoutingService.route(eq(caller), eq(operatorPhone)))
        .thenReturn(Future.successful(destination(RouteResults.Blocked)))
      val reaction = eventReactionService.react(callAcceptedEvent).futureValue
      reaction.action shouldEqual BlockAction(callAcceptedEvent.externalId, targetPhone)
      reaction.maybeResolution shouldEqual None
      reaction.maybeRedirect shouldEqual None
      Mockito.verify(callRoutingService).route(eq(caller), eq(operatorPhone))
    }

    "return RouteNoRedirectAction when redirect not found" in new Test {
      when(callRoutingService.route(eq(caller), eq(operatorPhone)))
        .thenReturn(Future.successful(destination(RouteResults.NoRedirect)))
      val reaction = eventReactionService.react(callAcceptedEvent).futureValue
      reaction.action shouldEqual RouteNoRedirectAction(callAcceptedEvent.externalId, targetPhone)
      reaction.maybeResolution shouldEqual None
      reaction.maybeRedirect shouldEqual None
      Mockito.verify(callRoutingService).route(eq(caller), eq(operatorPhone))
    }

    "return no action when receive non CallAcceptedEvent" in new Test {
      val reaction = eventReactionService.react(callAnsweredEvent).futureValue
      reaction.action shouldEqual NoAction(callAnsweredEvent.externalId)
      reaction.maybeResolution shouldEqual None
      reaction.maybeRedirect shouldEqual None
    }

    "redirect CallAcceptedEvent to target from redirect" in new Test {
      when(callRoutingService.route(eq(caller), eq(operatorPhone)))
        .thenReturn(Future.successful(destination(RouteResults.Passed)))
      val reaction = eventReactionService.react(callAcceptedEvent).futureValue
      reaction.action shouldEqual RedirectAction(callAcceptedEvent.externalId, targetPhone)
      reaction.maybeResolution shouldEqual None
      reaction.maybeRedirect shouldEqual None
      Mockito.verify(callRoutingService).route(eq(caller), eq(operatorPhone))
    }
  }

}
