package ru.yandex.vertis.billing.banker.ammo.helpers

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{`Content-Length`, Host}
import ru.yandex.vertis.billing.banker.ammo.Constants.Separator

trait BasePhantomRequestBuilder {

  def host: String

  // not override host passed in headers field
  def build(head: String, headers: Seq[HttpHeader], body: Option[String] = None, tag: String = ""): String = {
    val isHostExist = headers.exists {
      case _: Host => true
      case _ => false
    }
    val headersWithHost =
      if (!isHostExist) {
        Seq(Host(host)) ++ headers
      } else {
        headers
      }
    val fulfilledHeaders =
      if (body.isDefined) {
        headersWithHost :+ `Content-Length`(body.get.length + Separator.length)
      } else {
        headersWithHost
      }

    val b = body.map(b => s"$b$Separator").getOrElse("")
    val c = s"$head\n${fulfilledHeaders.mkString("\n")}$Separator$Separator$b"
    val t = Option(tag).filter(_.nonEmpty).map(t => s" $t").getOrElse("")
    s"${c.length}$t\n$c"
  }

}
