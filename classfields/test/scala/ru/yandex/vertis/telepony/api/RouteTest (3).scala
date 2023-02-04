package ru.yandex.vertis.telepony.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.directives.{LoggingMagnet, TimeoutDirectives}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

/**
  * @author evans
  */
trait RouteTest
  extends Matchers
  with Eventually
  with TimeoutDirectives
  with AnyWordSpecLike
  with ScalatestRouteTest
  with ScalaFutures {
  type RequestMagnet = LoggingMagnet[(HttpRequest) => (RouteResult) => Unit]

  val EmptyMagnet: RequestMagnet =
    LoggingMagnet.forRequestResponseFromFullShow((x: HttpRequest) => (y: RouteResult) => None)

  implicit def actorRefFactory: ActorSystem = system

  override def testConfig: Config = ConfigFactory.parseResources("spray.conf")

  implicit def default(implicit system: ActorSystem): RouteTestTimeout =
    RouteTestTimeout(300.millis.dilated(system))
}
