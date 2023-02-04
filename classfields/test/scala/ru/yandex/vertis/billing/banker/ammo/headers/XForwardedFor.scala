package ru.yandex.vertis.billing.banker.ammo.headers

import akka.http.scaladsl.model.headers.CustomHeader
import ru.yandex.vertis.billing.banker.model.SourceIP

case class XForwardedFor(user: SourceIP) extends CustomHeader {

  override def value(): SourceIP = user

  override def name(): SourceIP = "X-Forwarded-For"

  override def renderInResponses(): Boolean = true

  override def renderInRequests(): Boolean = true

}
