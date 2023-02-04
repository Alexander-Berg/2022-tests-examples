package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.generator.Generator.{PhoneGen, _}
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Action.{BlockAction, NoAction, RedirectAction}
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
class MtsEventReactionServiceImplSpec extends SpecBase {

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
    val operatorPhone = OperatorNumberGen.next.number
    val callAcceptedEvent = getCallAcceptedEvent(operatorPhone)
    val callAnsweredEvent = genCallAnsweredEvent(operatorPhone)
    val caller = callAcceptedEvent.sourceNumber

    val callRoutingService = mock[CallRoutingService]

    when(callRoutingService.route(eq(caller), eq(operatorPhone)))
      .thenReturn(Future.successful(Destination(targetPhone, RouteResults.Passed)))

    val eventReactionService =
      new MtsEventReactionServiceImpl(callRoutingService) with LoggingReactionService[Event, EventReaction] {
        override protected def operator: Operator = Operators.Mts
      }
  }

  "simple event reaction service" should {
    "pass all non CallAcceptedEvents" in new Test {
      val eventReaction = eventReactionService.react(callAnsweredEvent).futureValue
      eventReaction shouldEqual EventReaction(NoAction(callAnsweredEvent.externalId), Some(Pass))
      Mockito.verifyNoMoreInteractions(callRoutingService)
    }

    "pass when decider pass" in new Test {
      val eventReaction = eventReactionService.react(callAcceptedEvent).futureValue
      eventReaction shouldEqual EventReaction(NoAction(callAcceptedEvent.externalId))
      Mockito.verify(callRoutingService).route(eq(caller), eq(operatorPhone))
    }

    "block when decider block" in new Test {
      when(callRoutingService.route(eq(caller), eq(operatorPhone)))
        .thenReturn(Future.successful(Destination(targetPhone, RouteResults.Blocked)))
      val eventReaction = eventReactionService.react(callAcceptedEvent).futureValue
      eventReaction shouldEqual EventReaction(
        BlockAction(callAcceptedEvent.externalId, targetPhone)
      )
      Mockito.verify(callRoutingService).route(eq(caller), eq(operatorPhone))
    }
  }
}
