package ru.yandex.hydra.profile.api.v2.limiter

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import ru.yandex.hydra.profile.api.BaseHandlerSpec
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

/** Tests for [[LimiterHandler]]
  *
  * @author incubos
  */
class LimiterHandlerSpec extends BaseHandlerSpec {

  val route: Route = seal(
    new LimiterHandler(ForgettingDAOFactory, TestOperationalSupport.prometheusRegistry, ExecutionContext.global)
  )

  "/limiter" should {
    val USER = "user"

    "reject unsupported service" in {
      val rejectingRoute = seal(
        new LimiterHandler(
          new LimiterDAOFactory {
            override def get(service: String, locale: String, component: String) = None
          },
          TestOperationalSupport.prometheusRegistry,
          ExecutionContext.global
        )
      )

      Get(s"/service/locale/component/$USER") ~>
        rejectingRoute ~>
        check {
          status should equal(NotFound)
          responseAs[String] should include("service")
        }
    }

    "respond" in {
      Get(s"/service/locale/component/$USER") ~>
        route ~>
        check {
          status should equal(OK)
          responseAs[String] shouldBe "1"
        }
    }
  }
}
