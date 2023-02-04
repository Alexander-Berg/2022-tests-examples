package ru.auto.api.routes.v1.cme

import ru.auto.api.ApiSpec
import ru.auto.api.model.ModelGenerators.DealerSessionResultGen
import ru.auto.api.services.MockedClients
import akka.http.scaladsl.model.StatusCodes.OK
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.PromocodeModel.PromocodeListing
import ru.auto.api.model.AutoruDealer

class CmeHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  "promocode route" should {

    "return promocode listing from salesman" in {
      val session = DealerSessionResultGen.next
      when(passportClient.getSession(?)(?)).thenReturnF(session)

      val expected = PromocodeListing.getDefaultInstance
      when(salesmanClient.getPromocodeListing(eq(AutoruDealer(session.getUser.getClientId.toLong)))(?))
        .thenReturnF(expected)

      Get("/1.0/cme/promocode") ~>
        xAuthorizationHeader ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        route ~>
        check {
          status shouldBe OK
          val result = responseAs[PromocodeListing]
          result shouldBe expected
        }
    }
  }

}
