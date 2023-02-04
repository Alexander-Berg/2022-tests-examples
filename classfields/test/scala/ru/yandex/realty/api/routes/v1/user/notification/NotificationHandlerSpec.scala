package ru.yandex.realty.api.routes.v1.user.notification

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.api.ProtoResponse
import ru.yandex.realty.api.handlers.ExceptionsHandler
import ru.yandex.realty.api.routes._
import ru.yandex.realty.api.routes.v1.user.notification.NotificationHandler._
import ru.yandex.realty.auth.AuthRejectionHandler
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.notification.NotificationsManager
import ru.yandex.realty.model.exception.ForbiddenException
import ru.yandex.realty.model.user.{NoUser, PassportUser, UserInfo, UserRef, WebUser}
import ru.yandex.realty.request.Request
import ru.yandex.realty.user.notification._
import org.scalamock.matchers.Matchers

import scala.concurrent.Future

/**
  * Specs on [[NotificationHandler]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class NotificationHandlerSpec extends HandlerSpecBase with Matchers {

  private val manager: NotificationsManager = mock[NotificationsManager]

  val routeUnderTest: Route = new NotificationHandler(manager).route

  private val unauthorized = Future.failed(new ForbiddenException("artificial"))

  supportedMediaTypes.foreach { accept =>
    s"GET /user/notifications ($accept)" should {
      val request = Get("/user/notifications")
        .accepting(accept)

      "provide current notifications configuration" in {
        //TODO use generator
        val theConfigurations =
          NotificationConfiguration.configurations(NotificationConfiguration.offerChanges(push = true, email = false))

        val user = PassportUser(1)

        (manager
          .current(_: UserInfo, _: Set[DeliveryTypes.DeliveryType])(_: Request))
          .expects(where { (userInfo, deliveryTypes, _) =>
            userInfo.userRef == user && deliveryTypes == Set(DeliveryTypes.Push)
          })
          .returning(Future.successful(theConfigurations))

        request
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            entityAs[ProtoResponse.NotificationsResponse] should be(toResponse(theConfigurations))
          }
      }

      "respond Forbidden in case of UnauthorizedException" in {
        (manager
          .current(_: UserInfo, _: Set[DeliveryTypes.DeliveryType])(_: Request))
          .expects(*, *, *)
          .returning(unauthorized)
        ensureForbidden(request)
      }

      "respond Unauthorized if no user specified" in {
        ensureUnauthorized(request)
      }
    }

    s"PATCH /user/notification ($accept)" should {
      //TODO use generator
      val patch = NotificationConfiguration
        .offerChangesPushPatch(enabled = true)

      //TODO use generator
      val theConfiguration =
        NotificationConfiguration.offerChanges(push = true, email = true)

      val user = PassportUser(1)

      val request = Patch("/user/notification", patch)
        .accepting(accept)

      s"disable offer_changes pushes" in {
        (manager
          .update(_: UserInfo, _: ProtoNotification.NotificationConfiguration)(_: Request))
          .expects(where { (userInfo, _, _) =>
            userInfo.userRef == user
          })
          .returning(Future.successful(theConfiguration))

        request
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            entityAs[ProtoResponse.NotificationResponse] should be(toResponse(theConfiguration))
          }
      }

      s"respond Forbidden in case of UnauthorizedException" in {
        (manager
          .update(_: UserInfo, _: ProtoNotification.NotificationConfiguration)(_: Request))
          .expects(*, *, *)
          .returning(unauthorized)
        ensureForbidden(request)
      }

      s"respond Unauthorized if no user specified" in {
        ensureUnauthorized(request)
      }
    }

  }

  private def ensureUnauthorized(request: HttpRequest): Unit = {
    request
      .withUser(NoUser) ~>
      route ~>
      check {
        status should be(StatusCodes.Unauthorized)
      }
  }

  private def ensureForbidden(request: HttpRequest): Unit = {
    request
      .withUser(WebUser("1")) ~>
      route ~>
      check {
        status should be(StatusCodes.Forbidden)
      }
  }

  override protected def exceptionHandler: ExceptionHandler = ExceptionsHandler.myExceptionHandler

  override protected def rejectionHandler: RejectionHandler = AuthRejectionHandler.authRejectionHandler
}
