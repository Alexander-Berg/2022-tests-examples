package ru.yandex.vertis.telepony.api.v2.service.callback

import akka.http.scaladsl.marshalling.GenericMarshallers
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.GenericUnmarshallers
import org.mockito.Mockito
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eql}
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.api.v2.view.proto.ApiProtoConversions
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.CallbackGenerator._
import ru.yandex.vertis.telepony.model.CallbackOrder.CallbackOrderSource
import ru.yandex.vertis.telepony.model.{CallbackOrderPublic, TypedDomains}
import ru.yandex.vertis.telepony.service.CallbackOrderService
import ru.yandex.vertis.telepony.service.CallbackOrderService.{CancelResponse, ListResponse}
import ru.yandex.vertis.telepony.util.AuthorizedContext
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import scala.concurrent.Future

class CallbackHandlerSpec
  extends RouteTest
  with MockitoSupport
  with ScalaCheckDrivenPropertyChecks
  with GenericMarshallers
  with GenericUnmarshallers
  with ProtobufSupport
  with ApiProtoConversions {

  import ru.yandex.vertis.telepony.proto.CallbackOrderProto._

  val domain = TypedDomains.autoru_def

  def createHandler(cds: CallbackOrderService): Route = {
    seal(
      new CallbackHandler(cds, domain).route
    )
  }

  class TestEnv {
    val mockCS: CallbackOrderService = mock[CallbackOrderService]
    val handler: Route = createHandler(mockCS)

    implicit val ac: AuthorizedContext = AuthorizedContext(
      id = "1",
      login = "testLogin"
    )
  }

  "Callback handler" should {
    "create order" in new TestEnv {
      val createRequest = CallbackOrderRequestGen.next
      val order = CallbackOrderGen.next
      val callbackOrderSource = CallbackOrderSource.Api

      when(mockCS.order(?, eql(createRequest), eql(callbackOrderSource))(?)).thenReturn(Future.successful(order))
      val request: HttpRequest = Post(s"/order/create", createRequest)

      request ~> handler ~>
        check {
          responseAs[CallbackOrderPublic].toCallbackOrder should ===(order)
          status shouldEqual StatusCodes.OK
        }
      Mockito.verify(mockCS).order(?, eql(createRequest), eql(callbackOrderSource))(?)
    }

    "list orders" in new TestEnv {
      val orders = CallbackOrderGen.next(10).toList
      val listRequest = CallbackListRequestGen.next
      when(mockCS.list(eql(listRequest.asFilter(domain)), ?)(?)).thenReturn(Future.successful(orders))
      val request: HttpRequest = Post(s"/order/list", listRequest)

      request ~> handler ~>
        check {
          val response = responseAs[ListResponse]
          response.orders.map(_.toCallbackOrder) shouldEqual orders
          status shouldEqual StatusCodes.OK
        }
      Mockito.verify(mockCS).list(eql(listRequest.asFilter(domain)), ?)(?)
    }

    "cancel orders" in new TestEnv {
      val orders = CallbackOrderGen.next(10)
      val cancelRequest = CallbackCancelRequestGen.next
      val callbackCancelResponse = CancelResponse(orders.size)
      when(mockCS.cancel(?, eql(cancelRequest))(?)).thenReturn(Future.successful(callbackCancelResponse))
      val request: HttpRequest = Delete(s"/order/cancel", cancelRequest)

      request ~> handler ~>
        check {
          responseAs[CancelResponse] shouldEqual callbackCancelResponse
          status shouldEqual StatusCodes.OK
        }
      Mockito.verify(mockCS).cancel(?, eql(cancelRequest))(?)
    }
  }
}
