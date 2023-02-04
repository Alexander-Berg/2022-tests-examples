package ru.auto.cabinet.reporting.sender

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global

/** Should be used when needed custom Http server
  */
trait HttpEmulationHarness extends ScalaFutures {

  /** @param route   server logic
    * @param handler code executed in the context
    */
  def withHttpEmulation(route: Route)(
      handler: (String, ActorSystem, Materializer) => Unit): Unit = {

    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val patienceConfig: PatienceConfig = PatienceConfig(
      Span(10, Seconds))

    Http()
      .newServerAt("localhost", 0)
      .bindFlow(route)
      .map { binding: Http.ServerBinding =>
        // execute given logic
        handler(
          s"localhost:${binding.localAddress.getPort}",
          system,
          implicitly)
        binding
      }
      .flatMap(b => b.unbind())
      .flatMap(_ => system.terminate())
      .futureValue
  }
}
