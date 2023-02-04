package ru.yandex.vertis.telepony.api.v1.service.mtt

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.verify
import org.scalacheck.Gen
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.api.{RequestDirectives, RouteTest}
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.MttGenerator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.mtt.{EmptyResponse, StatusRequest}
import ru.yandex.vertis.telepony.service.impl.MttSharedReactionService
import ru.yandex.vertis.telepony.service.impl.mtt.Internals.JsonRpcResponse
import ru.yandex.vertis.telepony.view.MttJsonReactiveProtocol.{CallStatusEventFormat, CallStatusEventInt, GetControlCallFollowMe, GetControlCallFollowMeFormat}

import scala.concurrent.Future

/**
  * @author neron
  */
class MttHandlerSpec extends RouteTest with MockitoSupport with SprayJsonSupport {

  private trait TestHandler {
    val mockedReactionService = mock[MttSharedReactionService]

    val route = RequestDirectives.wrapRequest {
      RequestDirectives.seal(
        new MttHandler {
          override protected def reactionService: MttSharedReactionService = mockedReactionService
        }.route
      )
    }
  }

  "MttHandler" should {
    "handle call control request" in new TestHandler {
      import GetControlCallFollowMeFormat._
      val request = RoutingRequestGen.next
      val rawRequest = GetControlCallFollowMe.request(request)
      val jsonRequest = jsonRpcRequestGen(rawRequest).next

      val response = RoutingResponseGen.next
      val jsonResponse = JsonRpcResponse(
        jsonrpc = "2.0",
        result = Some(GetControlCallFollowMe.response(response)),
        error = None,
        id = jsonRequest.id
      )

      when(mockedReactionService.react(equ(request)))
        .thenReturn(Future.successful(response))

      Post("/", jsonRequest) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsonRpcResponse[GetControlCallFollowMe.Response]] should ===(jsonResponse)
      }
      verify(mockedReactionService).react(request)
    }

    "handle call event" in new TestHandler {
      import CallStatusEventFormat._
      val domain = Gen.option(DomainGen).next
      val request = StatusRequestGen.next
      val rawRequest = CallStatusEventInt.request(domain, request)

      val response = EmptyResponse

      def requestMatcher = ArgumentMatchers.argThat[StatusRequest] { req =>
        req.copy(time = request.time) == request
      }
      when(mockedReactionService.domainReact(equ(domain), requestMatcher))
        .thenReturn(Future.successful(response))

      Post("/", rawRequest) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe empty
      }

      verify(mockedReactionService).domainReact(equ(domain), requestMatcher)
    }
  }

}
