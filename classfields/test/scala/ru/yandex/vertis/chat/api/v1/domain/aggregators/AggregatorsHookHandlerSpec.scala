package ru.yandex.vertis.chat.api.v1.domain.aggregators

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import ru.yandex.vertis.chat.components.dao.aggregators.AggregatorsHookService
import ru.yandex.vertis.chat.service.ServiceProtoFormats
import ru.yandex.vertis.chat.api.HandlerSpecBase
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import ru.yandex.vertis.chat.util.DMap

class AggregatorsHookHandlerSpec
  extends HandlerSpecBase
  with ProducerProvider
  with ServiceProtoFormats
  with RequestContextAware
  with MockitoSupport {
  import AggregatorsHookHandler._

  val service: AggregatorsHookService = mock[AggregatorsHookService]
  val handler: AggregatorsHookHandler = new AggregatorsHookHandler(DMap.forAllDomains(service))
  private val route = seal(handler.route)

  private val entity =
    """{
      |   "sender" :
      |   {
      |    "id"    : "12346",
      |    "name"  : "John Doe",
      |    "photo" : "https://example.com/photo.jpg",
      |    "url"   : "https://ya.ru/simple/page.html",
      |    "phone" : "12345678901",
      |    "email" : "john@doe.сom",
      |    "invitation_text" : "Hello! Can I help you?"
      |   },
      |   "message" :
      |   {
      |    "type" : "text",
      |    "id"   : "124",
      |    "date" : 1496393172,
      |    "text" : "User Message Text 11"
      |   }
      |}""".stripMargin

  s"POST ${root}auto/hook" should {
    "create aggregator for user" in {
      when(service.aggregatorsHook(?, ?, ?)(?, ?)).thenReturn(Future.unit)
      Post(s"${root}auto/hook")
        .withEntity(HttpEntity(MediaTypes.`application/json`, entity))
        .withQueryParam(TokenParam, "token")
        .accepting(MediaTypes.`application/json`) ~> route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }
  }

}
