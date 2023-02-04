package ru.yandex.hydra.profile.api.v2.counter

import java.util.Random
import akka.http.scaladsl.server.Route
import ru.yandex.hydra.profile.api.BaseHandlerSpec
import akka.http.scaladsl.model.StatusCodes._
import ru.yandex.vertis.ops.test.TestOperationalSupport

/** Tests for [[CounterHandler]]
  *
  * @author incubos
  */
class CounterHandlerSpec extends BaseHandlerSpec {

  val route: Route = seal((tracing, logger) =>
    rt => new CounterHandler(ForgettingDAOFactory, TestOperationalSupport.prometheusRegistry, tracing, logger)(rt)
  )

  "/counter" should {
    val OID = "oid"
    val TOKEN = "token"

    "increment and get counter using GET" in {
      Get(s"/service/locale/component/$OID/$TOKEN") ~>
        route ~>
        check {
          status should equal(OK)
          responseAs[String] should equal("1")
        }
    }

    "reject unsupported service" in {
      val rejectingRoute = seal((tracing, logger) =>
        rt =>
          new CounterHandler(
            new TokenCounterDAOFactory {
              override def get(service: String, locale: String, component: String) = None
            },
            TestOperationalSupport.prometheusRegistry,
            tracing,
            logger
          )(rt)
      )

      Get(s"/service/locale/component/$OID/$TOKEN") ~>
        rejectingRoute ~>
        check {
          status should equal(NotFound)
          responseAs[String] should include("service")
        }
    }

    "get zero counter" in {
      val OBJECT = new Random().nextLong().toString

      Get(s"/service/locale/component/$OBJECT") ~>
        route ~>
        check {
          status should equal(OK)
          responseAs[String] should equal("0")
        }
    }

    "increment and get counter using PUT" in {
      val OBJECT = new Random().nextLong().toString

      Put(s"/service/locale/component/$OBJECT", "data") ~>
        route ~>
        check {
          status should equal(OK)
          responseAs[String] should equal("1")
        }
    }

    "just get zero counter using GET" in {
      val OBJECT = new Random().nextLong().toString

      Get(s"/service/locale/component/$OBJECT") ~>
        route ~>
        check {
          status should equal(OK)
          responseAs[String] should equal("0")
        }
    }

    "fail getting empty batch counter" in {
      Get(s"/service/locale/component") ~>
        route ~>
        check {
          status should equal(BadRequest)
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
