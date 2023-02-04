package ru.yandex.vertis.telepony.service.impl

import org.mockito.Mockito.verify
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.mtt.{EmptyResponse, RichRoutingRequest, RoutingRequest, RoutingResponse}
import ru.yandex.vertis.telepony.service.{DevNullPhonesService, MttReactionService, SharedPoolService}
import ru.yandex.vertis.telepony.util.Threads.lightWeightTasksEc

import scala.concurrent.Future

/**
  * @author neron
  */
class MttSharedReactionServiceSpec extends SpecBase {

  trait Test extends MockitoSupport {
    val mockedSharedPool = mock[SharedPoolService]
    val mockedReaction = mock[MttReactionService]
    val mockedDevNullPhones = mock[DevNullPhonesService]
    val eventUrl = ShortStr.next
    val service = new MttSharedReactionService(mockedSharedPool, _ => mockedReaction, mockedDevNullPhones, eventUrl)
  }

  "MttSharedReactionService" should {
    "react when domain is defined" in new Test {
      val sharedNumber = SharedOperatorNumberGen.suchThat(_.domain.isDefined).next
      val rr = RoutingRequest(sharedNumber.number, null, null)
      val rrr = RichRoutingRequest(sharedNumber, rr)
      when(mockedSharedPool.get(equ(sharedNumber.number))(?)).thenReturn(Future.successful(sharedNumber))
      when(mockedReaction.react(equ(rrr))).thenReturn(Future.successful(EmptyResponse))
      val response = service.react(rr).futureValue
      response should ===(EmptyResponse)
      verify(mockedSharedPool).get(equ(sharedNumber.number))(?)
      verify(mockedReaction).react(equ(rrr))
    }

    "react by default when domain is not defined" in new Test {
      val sharedNumber = SharedOperatorNumberGen.suchThat(_.domain.isEmpty).next
      val rr = RoutingRequest(sharedNumber.number, null, null)
      val target = PhoneGen.next
      val expectedResponse = RoutingResponse(
        targetNumber = target,
        statusEventUrl = eventUrl,
        callerAudio = None,
        targetAudio = None,
        domain = None
      )
      when(mockedSharedPool.get(equ(sharedNumber.number))(?)).thenReturn(Future.successful(sharedNumber))
      when(mockedDevNullPhones.get).thenReturn(target)
      val response = service.react(rr).futureValue
      response should ===(expectedResponse)
      verify(mockedSharedPool).get(equ(sharedNumber.number))(?)
      verify(mockedDevNullPhones).get
    }
  }

}
