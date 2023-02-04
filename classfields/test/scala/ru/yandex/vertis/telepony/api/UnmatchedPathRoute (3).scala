package ru.yandex.vertis.telepony.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path

/**
  * @author @logab
  */
case class UnmatchedPathRoute() {

  import akka.http.scaladsl.server.Directives._

  @volatile
  var unmatchedPath: Path = _

  val route = extractUnmatchedPath { path => http =>
    unmatchedPath = path
    http.complete(StatusCodes.OK)
  }
}
