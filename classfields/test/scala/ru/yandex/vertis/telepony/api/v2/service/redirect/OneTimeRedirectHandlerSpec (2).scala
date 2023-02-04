package ru.yandex.vertis.telepony.api.v2.service.redirect

import akka.http.scaladsl.model.{StatusCodes, Uri}
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.api.v2.view.proto.ApiProtoConversions
import ru.yandex.vertis.telepony.generator.Generator
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.service.OneTimeRedirectService
import ru.yandex.vertis.telepony.service.OneTimeRedirectService.OneTimeRedirect
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class OneTimeRedirectHandlerSpec extends RouteTest with MockitoSupport with ProtobufSupport with ApiProtoConversions {

  import MockitoSupport.{eq => eqq}

  trait Test {
    val service = mock[OneTimeRedirectService]
    val handler = new OneTimeRedirectHandler(service)
  }

  "OneTimeRedirectHandler" should {
    "create one-time redirect" in new Test {
      val request = Generator.OneTimeRedirectCreateRequestGen.next
      val response = Generator.OneTimeRedirectGen.next

      when(service.create(?)(?)).thenReturn(Future.successful(response))

      Post(Uri./, request) ~> seal(handler.route) ~> check {
        val actualResponse = responseAs[OneTimeRedirect]
        status shouldBe StatusCodes.OK
        actualResponse shouldBe response

        Mockito.verify(service).create(eqq(request))(?)
      }

    }
  }

}
