package ru.auto.cabinet.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

trait TestServer {
  val host = "localhost"

  def withServer[T](routes: Route)(block: Future[Uri] => T)(implicit
      system: ActorSystem,
      ec: ExecutionContext): T = {
    val server = Http().newServerAt(host, 0).bindFlow(routes)
    try block(
      server
        .map(_.localAddress)
        .map(addr =>
          Uri.from("http", host = addr.getHostString, port = addr.getPort)))
    finally server.flatMap(_.terminate(10.seconds))
  }
}
