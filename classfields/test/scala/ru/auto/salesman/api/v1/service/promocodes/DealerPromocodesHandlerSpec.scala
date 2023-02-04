package ru.auto.salesman.api.v1.service.promocodes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import ru.auto.api.PromocodeModel.PromocodeListing
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.model.AutoruDealer
import ru.auto.salesman.service.PromocodeListingService
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.testkit.TestDuration
import scala.concurrent.duration._

class DealerPromocodesHandlerSpec extends RoutingSpec {
  import DealerPromocodesHandlerSpec._

  implicit val timeout: RouteTestTimeout = RouteTestTimeout(5.seconds.dilated)

  def createRoute: (PromocodeListingService, Route) = {
    val listingService = mock[PromocodeListingService]
    val route = new DealerPromocodesHandler(listingService).route
    (listingService, route)
  }
  "GET /promocodes/client" should {
    "return listing" in {
      val (service, route) = createRoute

      val expected = PromocodeListing.getDefaultInstance

      (service.getListing _)
        .expects(AutoruDealer(testClientId))
        .returningZ(expected)

      Get(s"/client/$testClientId")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val res = responseAs[PromocodeListing]
        res shouldBe expected
      }

    }
  }

}

object DealerPromocodesHandlerSpec {
  private val testClientId = 123
}
