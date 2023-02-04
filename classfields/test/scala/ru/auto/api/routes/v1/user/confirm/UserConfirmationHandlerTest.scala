package ru.auto.api.routes.v1.user.confirm

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.mockito.Mockito.verify
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSuite
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators.{ConfirmIdentityParamsGen, ConfirmIdentityResultGen, PrivateUserRefGen}
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf
import ru.yandex.passport.model.api.ApiModel.ConfirmIdentityResult

/**
  *
  * @author zvez
  */
class UserConfirmationHandlerTest extends ApiSuite with ScalaCheckPropertyChecks with MockedClients {

  test("confirm identity") {
    forAll(PrivateUserRefGen, ConfirmIdentityParamsGen, ConfirmIdentityResultGen) { (user, params, result) =>
      when(passportClient.getUserEssentials(eq(user), ?)(?))
        .thenReturnF(ModelGenerators.UserEssentialsGen.next)
      when(passportClient.confirmIdentity(?)(?)).thenReturnF(result)

      Post(s"/1.0/user/confirm", params) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[ConfirmIdentityResult](response)
            responseParsed shouldBe result
          }
          verify(passportClient).confirmIdentity(eq(params))(?)
        }
    }
  }

}
