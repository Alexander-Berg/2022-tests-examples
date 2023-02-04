package ru.yandex.vertis.moisha.impl.autoru.example

import spray.json.JsValue

import scala.concurrent.Future

/**
  * Async Moisha client that operates low-level [[JsValue]]s.
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AsyncMoishaJsonClient {

  def post(body: JsValue): Future[JsValue]

}
