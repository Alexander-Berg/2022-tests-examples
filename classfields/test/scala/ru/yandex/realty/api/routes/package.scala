package ru.yandex.realty.api

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{HttpRequest, MediaType, MediaTypes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler}
import ru.yandex.realty.akka.http.directives.RequestDirectives
import ru.yandex.realty.api.handlers.{ExceptionsHandler, RejectionHandlers}
import ru.yandex.realty.auth.{Application, AuthInfo, AuthRejectionHandler}
import ru.yandex.realty.model.user._
import ru.yandex.realty.platform.PlatformInfo
import ru.yandex.realty.request.UserAgent
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf

import scala.language.implicitConversions

/**
  * @author dimas
  */
package object routes {

  /**
    * Supports domain-specific stuff injection to [[HttpRequest]]
    */
  implicit class RichRequest(val request: HttpRequest) extends AnyVal {

    def withPlatform(platformInfo: Option[PlatformInfo]): HttpRequest = {
      RequestDirectives
        .transformRequest(request) {
          _.setPlatformInfo(platformInfo)
        }
    }

    def withAuth(auth: AuthInfo): HttpRequest = {
      RequestDirectives
        .transformRequest(request) {
          _.setAuthInfo(auth)
        }
    }

    def withUser(ref: UserRef): HttpRequest = {
      val auth = ref match {
        case PassportUser(uid) => AuthInfo(uidOpt = Some(uid.toString))
        case WebUser(yandexUid) => AuthInfo(yandexUid = Some(yandexUid))
        case AppUser(uuid) => AuthInfo(uuid = Some(uuid))
        case NoUser => AuthInfo()
      }
      withAuth(auth)
    }

    def withApplication(app: Application): HttpRequest = {
      RequestDirectives
        .transformRequest(request) {
          _.setApplication(app)
        }
    }

    def withUserAgent(userAgent: Option[UserAgent]): HttpRequest = {
      RequestDirectives
        .transformRequest(request) {
          _.setUserAgent(userAgent)
        }
    }

    def accepting(mediaType: MediaType): HttpRequest = {
      request.addHeader(Accept(mediaType))
    }

    def acceptingJson: HttpRequest = {
      accepting(MediaTypes.`application/json`)
    }

    def acceptingProto: HttpRequest = {
      accepting(Protobuf.mediaType)
    }
  }

  def defaultExceptionHandler: ExceptionHandler = ExceptionsHandler.myExceptionHandler

  def defaultRejectionHandler: RejectionHandler =
    AuthRejectionHandler.authRejectionHandler
      .withFallback(new RejectionHandlers {}.commonRejectionHandler)
}
