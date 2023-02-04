package ru.yandex.vertis.telepony.api.v2.service.call

import akka.http.scaladsl.model.{StatusCodes, Uri}
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.api.v2.view.proto.ApiProtoConversions
import ru.yandex.vertis.telepony.generator.Generator
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.service.CallHistoryService
import ru.yandex.vertis.telepony.service.CallHistoryService.CallResponse
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import scala.concurrent.Future

/**
  *
  * @author neron
  */
class CallHistoryHandlerSpec extends RouteTest with MockitoSupport with ProtobufSupport with ApiProtoConversions {

  import MockitoSupport.{eq => eqq}

  trait Test {
    val service = mock[CallHistoryService]
    val handler = new CallHistoryHandler(service)
  }

  "CallHistoryHandler" should {
    "list call history" in new Test {
      val request = Generator.CallRequestGen.next
      val response = Generator.CallResponseGen.next

      when(service.list(?)(?)).thenReturn(Future.successful(response))

      Post(Uri./, request) ~> seal(handler.route) ~> check {
        val actualResponse = responseAs[CallResponse]
        status shouldBe StatusCodes.OK
        actualResponse shouldBe response

        Mockito.verify(service).list(eqq(request))(?)
      }

    }
  }

}
