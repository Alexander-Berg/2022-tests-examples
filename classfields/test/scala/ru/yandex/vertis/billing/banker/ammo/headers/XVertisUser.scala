package ru.yandex.vertis.billing.banker.ammo.headers

import akka.http.scaladsl.model.headers.CustomHeader
import ru.yandex.vertis.billing.banker.model.User

case class XVertisUser(user: User) extends CustomHeader {

  override def value(): User = user

  override def name(): User = "X-Vertis-User"

  override def renderInResponses(): Boolean = true

  override def renderInRequests(): Boolean = true

}
