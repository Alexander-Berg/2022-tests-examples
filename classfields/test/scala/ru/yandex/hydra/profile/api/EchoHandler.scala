package ru.yandex.hydra.profile.api

import akka.http.scaladsl.server.Route

/** Responds with passed entity.
  *
  * @author dimas
  */
object EchoHandler extends BaseHttpHandler {

  override val route: Route = entity(as[String]) { value =>
    complete(value)
  }
}
