package ru.yandex.vertis.telepony.api.v1.service.beeline

import akka.http.scaladsl.model.StatusCodes
import org.joda.time.DateTime
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.api.{RequestDirectives, RouteTest}
import ru.yandex.vertis.telepony.model.beeline._
import ru.yandex.vertis.telepony.model.{DomainEvent, Phone, Source, TypedDomains}
import ru.yandex.vertis.telepony.service.impl.BeelineSharedReactionService
import ru.yandex.vertis.telepony.view.BeelineXmlProtocol._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author neron
  */
class BeelineHandlerSpec extends RouteTest with MockitoSupport {

  import AgentInfo.agentInfoUnmarshaller
  import AgentRequest.agentRequestMarshaller
  import CallStatusXml.callStatusMarshaller

  private trait TestHandler {
    val mockedReactionService = mock[BeelineSharedReactionService]

    val route = RequestDirectives.wrapRequest {
      RequestDirectives.seal(
        new BeelineHandler {
          override protected def reactionService: BeelineSharedReactionService = mockedReactionService
        }.route
      )
    }

  }

  private class AgentRequestTestHandler(emptySource: Boolean = false) extends TestHandler {
    val source = Some(Source("4959743581")).filterNot(_ => emptySource)
    val target = Phone("+79817757583")

    val agentRequest = RoutingRequest(
      scriptId = "script_id",
      hostId = HostId("host_id"),
      sysId = SysId("sys_id"),
      callerNumber = source,
      operatorNumber = Phone("+79647624288")
    )

    val agentInfo = RoutingResponse(
      callId = CallId.from("some call id"),
      domain = Some(TypedDomains.autoru_def),
      callerNumber = agentRequest.callerNumber,
      targetNumber = target,
      scriptId = agentRequest.scriptId,
      hostId = agentRequest.hostId,
      recordEnabled = true,
      callerAudio = Some("caller_audio_file.wav"),
      targetAudio = Some("target_audio_file.wav"),
      additionalParameter = None
    )

    when(mockedReactionService.react(equ(agentRequest)))
      .thenReturn(Future.successful(agentInfo))

    Post("/", agentRequest) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[RoutingResponse] should ===(agentInfo)
    }

  }

  "BeelineHandler" should {

    "handle AgentRequest" in new AgentRequestTestHandler()

    "handle AgentRequest with empty source" in new AgentRequestTestHandler(emptySource = true)

    "handle CallStatus event" in new TestHandler {
      val callStatus = StatusRequest(
        externalId = "host_id+script_id",
        callerState = CallStates.Connected,
        calleeState = CallStates.Calling,
        reason = -1,
        time = DateTime.now().withMillis(0),
        talkDuration = 0.seconds,
        additionalParameter = "abc"
      )
      val domainOpt = Some(TypedDomains.autoru_def)
      val domainCallStatus = DomainEvent(domainOpt, callStatus)

      when(mockedReactionService.domainReact(domainOpt, callStatus)).thenReturn(Future.successful(EmptyResponse))

      Post("/", domainCallStatus) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

}
