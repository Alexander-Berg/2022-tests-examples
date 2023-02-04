package ru.yandex.vos2.reviews.api.handlers

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.scalatest.{FunSuite, Matchers}
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport
import ru.yandex.vos2.util.log.Logging
import ru.yandex.vos2.reviews.utils.InitReviewTestDb

import scala.concurrent.duration.DurationInt

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 05/10/2017.
  */
trait BaseReviewHandlerTest extends FunSuite with Matchers with InitReviewTestDb with
  Logging with ScalatestRouteTest with ProtobufSupport {

  implicit def default(implicit system: ActorSystem): RouteTestTimeout = {
    RouteTestTimeout(new DurationInt(6000).second)
  }

  private val rootHandler = new TestRout

  val route: Route = rootHandler.rout

}
