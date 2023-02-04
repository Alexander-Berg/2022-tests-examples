package ru.yandex.vertis.billing.banker.ammo.helpers

import akka.http.scaladsl.model.{ContentType, HttpHeader}
import akka.http.scaladsl.model.headers.{`Content-Length`, `Content-Type`, Host}
import ru.yandex.vertis.billing.banker.ammo.headers.{XForwardedFor, XVertisUser}
import ru.yandex.vertis.billing.banker.ammo.Constants.DefaultHeaders
import ru.yandex.vertis.billing.banker.model.{SourceIP, User}

trait HeaderHelpers {

  def customHeaders(
      vertisUser: Option[User] = None,
      host: Option[String] = None,
      contentType: Option[ContentType] = None,
      xForwardedFor: Option[SourceIP] = None): List[HttpHeader] = {
    val custom = List(
      host.map(address => Host(address)),
      contentType.map(t => `Content-Type`(t)),
      vertisUser.map(XVertisUser),
      xForwardedFor.map(XForwardedFor)
    )
    custom.flatten ++ DefaultHeaders
  }

}
