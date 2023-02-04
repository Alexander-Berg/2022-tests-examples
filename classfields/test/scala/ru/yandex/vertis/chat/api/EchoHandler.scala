package ru.yandex.vertis.chat.api

import akka.http.scaladsl.server.Route
import ru.yandex.vertis.chat.api.util.routes.BaseHandler

/**
  * Responds with passed entity.
  *
  * @author dimas
  */
object EchoHandler extends BaseHandler {

  override val route: Route = entity(as[String]) { value =>
    complete(value)
  }
}
