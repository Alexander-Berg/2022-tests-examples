package vertis.statist.api.http

import akka.http.scaladsl.server.Route

/** Responds with passed entity.
  *
  * @author dimas
  */
object EchoHandler extends BaseHandler {

  override val route: Route = entity(as[String]) { value =>
    complete(value)
  }
}
