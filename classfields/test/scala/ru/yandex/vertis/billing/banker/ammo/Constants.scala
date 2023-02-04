package ru.yandex.vertis.billing.banker.ammo

import akka.http.scaladsl.model.headers.{Accept, Connection}
import akka.http.scaladsl.model.{ContentTypes, HttpHeader}

object Constants {

  val Protocol = "HTTP/1.1"

  val Separator = "\r\n"

  val AccountPrefix = "stress"

  val SuperUser = "stress-super-user"

  val DefaultHeaders: List[HttpHeader] =
    Connection("Keep-Alive") ::
      Accept(ContentTypes.`application/json`.mediaType) ::
      Nil

}
