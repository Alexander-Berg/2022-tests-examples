package ru.yandex.vertis.billing.api.routes.main.v1.service.balance

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Exceptions.ArtificialInternalException
import ru.yandex.vertis.billing.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.api.routes.main.v1.service.balance.Handler
import ru.yandex.vertis.billing.api.view.ClientView
import ru.yandex.vertis.billing.balance.model.{Client, ClientId, ClientProperties, ClientTypes}
import ru.yandex.vertis.billing.model_core.{Login, Uid, User}
import ru.yandex.vertis.billing.util.OperatorContext
import spray.json._

import scala.concurrent.Future

/** Specs balance handler - [[Handler]]
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/balance"

  val login = Login("login")
  val clientName = "Test Client"
  val client = Client(1, ClientProperties(ClientTypes.IndividualPerson, name = Some(clientName)))

  "GET /client" should {

    "not try to find client without specified operator (via header)" in {
      Get(url("/client?uid=1")) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    "not find client by uid" in {
      stub(backend.balanceApi.findByUser(_: User)(_: OperatorContext)) { case (`uid`, `operator`) =>
        Future.successful(None)
      }
      Get(url(s"/client?uid=${uid.id}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.NotFound)
        }
    }

    "find client by uid" in {
      stub(backend.balanceApi.findByUser(_: User)(_: OperatorContext)) { case (`uid`, `operator`) =>
        Future.successful(Some(client))
      }
      Get(url(s"/client?uid=${uid.id}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val clientView = responseAs[ClientView]
          clientView should be(ClientView(client))
        }
    }

    "find client by login" in {
      stub(backend.balanceApi.findByUser(_: User)(_: OperatorContext)) { case (`login`, `operator`) =>
        Future.successful(Some(client))
      }
      Get(url(s"/client?login=${login.login}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val clientView = responseAs[ClientView]
          clientView should be(ClientView(client))
        }
    }

    "respond InternalServerError in case of backend problems" in {
      stub(backend.balanceApi.findByUser(_: User)(_: OperatorContext)) { case (`login`, `operator`) =>
        Future.failed(ArtificialInternalException())
      }
      Get(url(s"/client?login=${login.login}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.InternalServerError)
        }
    }
  }

  "GET /client/{id}" should {

    "not try to get client without specified operator (via header)" in {
      Get(url(s"/client/${client.id}")) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    "not get client by ID" in {
      stub(backend.balanceApi.get _) { case (client.`id`) =>
        Future.successful(None)
      }
      Get(url(s"/client/${client.id}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.NotFound)
        }
    }

    "get client by ID" in {
      stub(backend.balanceApi.get _) { case (client.`id`) =>
        Future.successful(Some(client))
      }
      Get(url(s"/client/${client.id}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val clientView = responseAs[ClientView]
          clientView should be(ClientView(client))
        }
    }

  }

  "POST /client" should {

    "not try to create client without specified operator (via header)" in {
      Post(url("/client")) ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }

    "create client" in {
      stub(backend.balanceApi.create(_: ClientProperties, _: Option[ClientId])(_: OperatorContext)) {
        case (client.`properties`, _, `operator`) =>
          Future.successful(client)
      }
      val content = ClientView(client).toJson.compactPrint
      val entity = HttpEntity(ContentTypes.`application/json`, content)
      Post(url("/client"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          responseAs[ClientView] should be(ClientView(client))
        }
    }

  }

  "/client/{client}/user/{uid}" should {

    "assign user to client" in {
      stub(backend.balanceApi.assignUser(_: Uid, _: ClientId)(_: OperatorContext)) {
        case (`uid`, client.`id`, `operator`) =>
          Future.successful(())
      }
      Post(url(s"/client/${client.id}/user/${uid.id}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "deassign user to client" in {
      stub(backend.balanceApi.deassignUser(_: Uid, _: ClientId)(_: OperatorContext)) {
        case (`uid`, client.`id`, `operator`) =>
          Future.successful(())
      }
      Delete(url(s"/client/${client.id}/user/${uid.id}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }
  }

  "GET /client/{client}/user" should {
    stub(backend.balanceApi.getClientDelegates(_: ClientId)(_: OperatorContext)) { case (client.`id`, `operator`) =>
      Future.successful(Iterable.empty)
    }
    "list balance users" in {
      Get(url(s"/client/${client.id}/user")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }
  }
}
