package ru.yandex.vertis.billing.api.routes.main.v1.service.role

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Exceptions.ArtificialInternalException
import ru.yandex.vertis.billing.api.Exceptions.artificialAccessDenyException
import ru.yandex.vertis.billing.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.api.routes.main.v1.service.role.Handler
import ru.yandex.vertis.billing.model_core.{Uid, User}
import ru.yandex.vertis.billing.service.RoleService
import ru.yandex.vertis.billing.service.RoleService.UserRole
import ru.yandex.vertis.billing.util.{OperatorContext, RequestContext}

import scala.annotation.nowarn
import scala.concurrent.Future

/** Specs on roles handler [[Handler]]
  */
@nowarn("msg=discarded non-Unit value")
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/role"
  import ru.yandex.vertis.billing.api.view.UserRoleView.modelUnmarshaller

  val user = Uid(1)

  "GET /?uid={}" should {
    "provide RegularUser role" in {
      provideUidRole(user, RoleService.Roles.RegularUser)
    }
    "provide SuperUser role" in {
      provideUidRole(user, RoleService.Roles.SuperUser)
    }
    "fail to respond in" in {
      stub(backend.roleService.get(_: User)(_: RequestContext)) { case (`user`, _) =>
        Future.failed(ArtificialInternalException())
      }
      Get(url(s"/?uid=${user.id}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.InternalServerError)
        }
    }
  }

  "GET /?user={}" should {
    "provide RegularUser role" in {
      provideUserRole(user, RoleService.Roles.RegularUser)
    }
    "provide SuperUser role" in {
      provideUserRole(user, RoleService.Roles.SuperUser)
    }
    "fail to respond in" in {
      stub(backend.roleService.get(_: User)(_: RequestContext)) { case (`user`, _) =>
        Future.failed(ArtificialInternalException())
      }
      Get(url(s"/?user=${user.value}")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.InternalServerError)
        }
    }
  }

  "PUT /?uid={}&role={}" should {
    import ru.yandex.vertis.billing.service.RoleService.Roles._
    "not set role without operator header" in {
      Put(url(s"/?uid=${user.id}&role=$SuperUser")) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    "not set role by non-super user" in {
      stub(backend.roleService.assign(_: User, _: RoleService.Role)(_: OperatorContext)) {
        case (`user`, RoleService.Roles.`SuperUser`, `operator`) =>
          Future.failed(artificialAccessDenyException(operator.operator))
      }

      Put(url(s"/?uid=${user.id}&role=$SuperUser")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.Forbidden)
        }
    }
    "set role by super user with uid" in {
      val userRole = UserRole(user, SuperUser)
      stub(backend.roleService.assign(_: User, _: RoleService.Role)(_: OperatorContext)) {
        case (`user`, RoleService.Roles.`SuperUser`, `operator`) =>
          Future.successful(userRole)
      }
      Put(url(s"/?uid=${user.id}&role=$SuperUser")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          responseAs[UserRole] shouldBe userRole
        }
    }
    "set role by super user with user" in {
      val userRole = UserRole(user, SuperUser)
      stub(backend.roleService.assign(_: User, _: RoleService.Role)(_: OperatorContext)) {
        case (`user`, RoleService.Roles.`SuperUser`, `operator`) =>
          Future.successful(userRole)
      }

      Put(url(s"/?user=${user.value}&role=$SuperUser")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          responseAs[UserRole] shouldBe userRole
        }
    }
  }

  private def provideUidRole(user: Uid, role: RoleService.Role): Unit = {
    stub(backend.roleService.get(_: User)(_: RequestContext)) { case (`user`, _) =>
      Future.successful(UserRole(user, role))
    }
    Get(url(s"/?uid=${user.id}")) ~> route ~> check {
      status should be(StatusCodes.OK)
      val userRole = responseAs[UserRole]
      userRole.user shouldBe user
      userRole.role shouldBe role
    }
  }

  private def provideUserRole(user: User, role: RoleService.Role): Unit = {
    stub(backend.roleService.get(_: User)(_: RequestContext)) { case (`user`, _) =>
      Future.successful(UserRole(user, role))
    }
    Get(url(s"/?user=${user.value}")) ~> route ~> check {

      status should be(StatusCodes.OK)
      val userRole = responseAs[UserRole]
      userRole.user shouldBe user
      userRole.role shouldBe role
    }
  }
}
