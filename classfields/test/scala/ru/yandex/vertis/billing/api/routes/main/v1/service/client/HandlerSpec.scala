package ru.yandex.vertis.billing.api.routes.main.v1.service.client

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Exceptions.ArtificialInternalException
import ru.yandex.vertis.billing.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.api.routes.main.v1.service.client.Handler
import ru.yandex.vertis.billing.api.routes.main.v1.view.ClientDescriptionView
import ru.yandex.vertis.billing.api.view.ClientView
import ru.yandex.vertis.billing.balance.model.{Client, ClientId, ClientProperties, ClientTypes}
import ru.yandex.vertis.billing.model_core.Login
import ru.yandex.vertis.billing.service.ClientService.{ClientDescription, Query}
import ru.yandex.vertis.billing.util.{OperatorContext, Page, Slice, SlicedResult}
import ru.yandex.vertis.billing.model_core.gens.notifyClientGen
import ru.yandex.vertis.billing.model_core.gens.Producer
import spray.json._

import scala.concurrent.Future

/**
  * @author ruslansd
  *
  * Specs balance handler -
  * [[Handler]]
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/client"

  val login = Login("login")
  val clientName = "Test Client"
  val client = Client(1, ClientProperties(ClientTypes.IndividualPerson, name = Some(clientName)))

  val clientDescription =
    ClientDescription(client.id, None, client, None, isAgencyInService = false)

  "GET /" should {

    "find clients or agencies with operator Uid (via header)" in {
      stub(backend.clientService.find(_: Query, _: Slice)(_: OperatorContext)) {
        case (_, s @ Page(0, 10), `operator`) =>
          Future.successful(SlicedResult(List(clientDescription), 1, s))
      }
      Get(url("/")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val views = responseAs[SlicedResult[ClientDescriptionView]]
          views.size should be(1)
          views.head should be(ClientDescriptionView(clientDescription))
        }
    }

    "not try to find client without specified operator (via header)" in {
      Get(url("/")) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    "respond InternalServerError in case of backend problems" in {
      stub(backend.clientService.find(_: Query, _: Slice)(_: OperatorContext)) {
        case (_, s @ Page(0, 10), `operator`) =>
          Future.failed(ArtificialInternalException())
      }
      Get(url("/")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.InternalServerError)
        }
    }

    "notify client" in {
      val notifyClient = notifyClientGen().next
      stub(backend.asyncNotifyClientService.lastForClient(_: ClientId)) { case _ =>
        Future(Some(notifyClient))
      }
      Get(url("/notifyClient/123")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val r = responseAs[String]
          r.length should not be 0
        }
    }

    "notify client 404 on empty" in {
      stub(backend.asyncNotifyClientService.lastForClient(_: ClientId)) { case _ =>
        Future(None)
      }
      Get(url("/notifyClient/123")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.NotFound
        }
    }

  }

  "POST /" should {
    val content = ClientView(client).toJson.compactPrint
    val entity = HttpEntity(ContentTypes.`application/json`, content)
    stub(backend.clientService.create(_: ClientProperties)(_: OperatorContext)) {
      case (client.`properties`, `operator`) =>
        Future.successful(clientDescription)
    }
    "create client" in {
      Post(url("/"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          responseAs[ClientDescriptionView] should be
          ClientDescriptionView(clientDescription)
        }
    }

    "not try to create client without specified operator (via header)" in {
      Post(url("/"), entity) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    "respond InternalServerError in case of backend problems" in {
      stub(backend.clientService.create(_: ClientProperties)(_: OperatorContext)) { case _ =>
        Future.failed(ArtificialInternalException())
      }
      Post(url("/"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.InternalServerError)
        }
    }
  }
}
