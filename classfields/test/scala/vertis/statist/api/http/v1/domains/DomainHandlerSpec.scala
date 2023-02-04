package vertis.statist.api.http.v1.domains

import akka.http.scaladsl.model.StatusCodes
import vertis.statist.api.http.{BaseHandlerSpec, EchoHandler, Handler}

/** @author zvez
  */
class DomainHandlerSpec extends BaseHandlerSpec {

  private val route = seal(
    new DomainHandler {
      override def countersHandler: Handler = EchoHandler
    }
  )

  s"GET /counters" should {
    val entity = "foo"
    "route to counters handler" in {
      Get(s"${Root}counters/some", entity) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[String] should be(entity)
      }
    }
  }

}
