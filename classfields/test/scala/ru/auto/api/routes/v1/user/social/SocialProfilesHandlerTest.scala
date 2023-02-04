package ru.auto.api.routes.v1.user.social

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.mockito.Mockito.{reset, verify}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSuite
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators.{PrivateUserRefGen, ReadableStringGen, SocialProviderGen}
import ru.auto.api.services.{MockedClients, MockedPassport}
import ru.auto.api.util.ManagerUtils.SuccessResponse

/**
  *
  * @author zvez
  */
class SocialProfilesHandlerTest extends ApiSuite with ScalaCheckPropertyChecks with MockedClients with MockedPassport {

  before {
    reset(passportManager)
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
  }

  test("addSocialProfile") {
    forAll(PrivateUserRefGen, ModelGenerators.PassportAddSocialProfileParamsGen) { (user, params) =>
      when(passportManager.addSocialProfile(?, ?)(?))
        .thenReturnF(SuccessResponse)

      Post(s"/1.0/user/social-profiles", params) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }

          verify(passportManager).addSocialProfile(eq(user), eq(params))(?)
        }
    }

  }

  test("removeSocialProfile") {
    forAll(PrivateUserRefGen, SocialProviderGen, ReadableStringGen) { (user, provider, socialUserId) =>
      when(passportManager.removeSocialProfile(?, ?, ?)(?))
        .thenReturnF(SuccessResponse)

      Delete(s"/1.0/user/social-profiles/$provider/$socialUserId") ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }

          verify(passportManager).removeSocialProfile(eq(user), eq(provider), eq(socialUserId))(?)
        }
    }

  }

}
