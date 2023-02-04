package ru.auto.salesman.test

import java.net.URI

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import org.scalatest.concurrent.ScalaFutures
import ru.auto.salesman.test.TestAkkaComponents._

import scala.concurrent.ExecutionContext.Implicits.global

trait TestHttpServer extends ScalaFutures {

  // return URI instead of String for better type-safety
  def runServer(route: Route): URI =
    Http()
      .bindAndHandle(route, "localhost", 0)
      // localAddress returns string with / in the beginning
      .map(server => URI.create(s"http:/${server.localAddress}"))
      .futureValue
}
