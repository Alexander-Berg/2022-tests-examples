package ru.yandex.vertis.billing.banker.api.v1.service.bootstrap

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.api.base_admin.RootHandlerSpecBase
import ru.yandex.vertis.billing.banker.api.v1.view.AccountBootstrapSourceView
import ru.yandex.vertis.billing.banker.model.Account
import ru.yandex.vertis.billing.banker.service.AccountBootstrapService.Source
import ru.yandex.vertis.billing.banker.util.RequestContext

import scala.concurrent.Future
import spray.json.enrichAny

/**
  * Spec on [[Handler]]
  *
  * @author alex-kovalenko
  */
class HandlerSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/bootstrap"

  "post /account" should {
    "create account from bootstrap source" in {
      stub(backend.accountBootstrap.get.create(_: Source)(_: RequestContext)) { case (s, _) =>
        Future.successful(s.account)
      }

      val account = Account("accId", "user")
      val source = Source(account, 0L, Iterable.empty)
      val entity = AccountBootstrapSourceView.asView(source).toJson.compactPrint
      Post(url("/account"))
        .withEntity(ContentTypes.`application/json`, entity) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
        import ru.yandex.vertis.billing.banker.api.v1.view.AccountView.modelUnmarshaller
        responseAs[Account] shouldBe account
      }
    }
  }
}
