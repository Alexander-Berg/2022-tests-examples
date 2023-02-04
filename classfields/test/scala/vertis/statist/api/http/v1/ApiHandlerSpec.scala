package vertis.statist.api.http.v1

import akka.http.scaladsl.model.StatusCodes
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.statist.api.http.{BaseHandlerSpec, EchoHandler}
import vertis.statist.model.Domain
import zio._

/** @author zvez
  */
class ApiHandlerSpec extends BaseHandlerSpec {

  private val TestDomain = Domain("test")

  private val route = seal(new ApiHandler {

    implicit override val runtime: zio.Runtime[ZEnv] = Runtime.default

    override def registry: PrometheusRegistry = TestOperationalSupport.prometheusRegistry

    override def domainHandlers = Map(
      TestDomain -> EchoHandler
    )
  })

  s"ANY $Root<ns>" should {
    val entity = "foo"
    "route to known namespace" in {
      Post(s"$Root$TestDomain", entity) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[String] should be(entity)
      }
    }
    "respond 404 for unknown namespace" in {
      Get(s"${Root}something") ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
  }

}
