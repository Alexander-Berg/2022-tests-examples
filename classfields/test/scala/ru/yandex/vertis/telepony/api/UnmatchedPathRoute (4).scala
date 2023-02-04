package ru.yandex.vertis.telepony.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.Route

/** Helps to write test probes on different routers
  */
case class UnmatchedPathRoute() {

  import akka.http.scaladsl.server.Directives._

  @volatile
  var unmatchedPath: Path = _

  val route: Route = extractUnmatchedPath { path => http =>
    unmatchedPath = path
    http.complete(StatusCodes.OK)
  }
}
