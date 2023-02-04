package ru.auto.api.routes.v1.user.moderation

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSuite
import ru.auto.api.ResponseModel.UserModerationStatusResponse
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators.{userModerationStatusResponseGen, PrivateUserRefGen}
import ru.auto.api.services.MockedClients

class ModerationHandlerSpec extends ApiSuite with MockedClients with ScalaCheckPropertyChecks {

  private val commonHeaders =
    xAuthorizationHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  test("get moderation status") {
    forAll(userModerationStatusResponseGen) { response =>
      val user = PrivateUserRefGen.next
      when(passportClient.getUserEssentials(eq(user), ?)(?))
        .thenReturnF(ModelGenerators.UserEssentialsGen.next)
      when(passportClient.getUserModeration(eq(user))(?))
        .thenReturnF(response.getModerationStatus)

      Get(s"/1.0/user/moderation/status") ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[UserModerationStatusResponse]
          response shouldBe response
        }
    }
  }
}
