package ru.yandex.hydra.profile.api.v2.clicker

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import ru.yandex.hydra.profile.api.BaseHandlerSpec
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

/** Tests for [[ClickerHandler]]
  *
  * @author incubos
  */
class ClickerHandlerSpec extends BaseHandlerSpec {

  val route: Route = seal(
    new ClickerHandler(ForgettingDAOFactory, TestOperationalSupport.prometheusRegistry, ExecutionContext.global)
  )

  "/clicker" should {
    val OID = "oid"

    "GET zero counter value" in {
      Get(s"/service/locale/component/$OID") ~>
        route ~>
        check {
          status shouldBe OK
          responseAs[String] shouldBe "0"
        }
    }

    "ignore GET collection" in {
      Get(s"/service/locale/component/$OID/") ~>
        route ~>
        check {
          status shouldBe NotFound
        }
    }

    "reject unsupported service" in {
      val rejectingRoute = seal(
        new ClickerHandler(
          new ClickerDAOFactory {
            override def get(service: String, locale: String, component: String) = None
          },
          TestOperationalSupport.prometheusRegistry,
          ExecutionContext.global
        )
      )

      Get(s"/service/locale/component/$OID") ~>
        rejectingRoute ~>
        check {
          status should equal(NotFound)
          responseAs[String] should include("service")
        }
    }

    "increment and get counter using PUT" in {
      Put(s"/service/locale/component/$OID") ~>
        route ~>
        check {
          status shouldBe Created
        }
    }

    "ignore PUT collection" in {
      Put(s"/service/locale/component/$OID/") ~>
        route ~>
        check {
          status shouldBe NotFound
        }
    }

    "ignore PUT body" in {
      Put(s"/service/locale/component/$OID", "body") ~>
        route ~>
        check {
          status shouldBe NotFound
        }
    }

    "increment and get counter using POST" in {
      Post(s"/service/locale/component/$OID") ~>
        route ~>
        check {
          status shouldBe Created
          responseAs[String] should equal("1")
        }
    }

    "ignore POST collection" in {
      Post(s"/service/locale/component/$OID/") ~>
        route ~>
        check {
          status shouldBe NotFound
        }
    }

    "ignore POST body" in {
      Post(s"/service/locale/component/$OID", "body") ~>
        route ~>
        check {
          status shouldBe NotFound
        }
    }

    "fail getting empty batch counter" in {
      Get(s"/service/locale/component") ~>
        route ~>
        check {
          status should equal(BadRequest)
        }
    }

    "fail getting collection" in {
      Get(s"/service/locale/component/") ~>
        route ~>
        check {
          status shouldBe NotFound
        }
    }

    "get one multi ID" in {
      Get(s"/service/locale/component?id=id") ~>
        route ~>
        check {
          status should equal(OK)
          responseAs[String] should include("\"id\":0")
        }
    }

    "get two multi IDs" in {
      Get(s"/service/locale/component?id=id1&id=id2") ~>
        route ~>
        check {
          status should equal(OK)
          val response = responseAs[String]
          response should include("\"id1\":0")
          response should include("\"id2\":0")
        }
    }
  }
}
