package ru.yandex.vertis.telepony.api

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

/**
  * @author evans
  */
trait RouteTest extends Matchers with AnyWordSpecLike with ScalatestRouteTest {

  implicit def actorRefFactory: ActorSystem = system

  override def testConfig: Config = ConfigFactory.empty()

  implicit def default(implicit system: ActorSystem): RouteTestTimeout =
    RouteTestTimeout(300.millis.dilated(system))
}
