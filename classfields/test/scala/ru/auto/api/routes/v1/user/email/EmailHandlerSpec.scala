package ru.auto.api.routes.v1.user.email

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import org.mockito.Mockito.verify
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.AddIdentityResponse
import ru.auto.api.managers.passport.PassportModelConverters.AutoruConvertable
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import ru.yandex.passport.model.api.ApiModel.RequestEmailChangeResult

/**
  *
  * @author zvez
  */
class EmailHandlerSpec extends ApiSpec with ScalaCheckPropertyChecks with MockedClients {

  private val commonHeaders =
    xAuthorizationHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  "request confirmation code for email change" should {
    "work" in {
      forAll(PrivateUserRefGen, PassportRequestEmailChangeParamsGen, PassportRequestEmailChangeResultGen) {
        (user, params, result) =>
          when(passportClient.getUserEssentials(eq(user), ?)(?))
            .thenReturnF(ModelGenerators.UserEssentialsGen.next)
          when(passportClient.requestEmailChangeCode(?, ?)(?))
            .thenReturnF(result)

          Post(s"/1.0/user/email/request-change-code", params) ~>
            addHeader("x-uid", user.uid.toString) ~>
            commonHeaders ~>
            route ~>
            check {
              withClue(responseAs[String]) {
                status shouldBe StatusCodes.OK
                contentType shouldBe ContentTypes.`application/json`

                val response = responseAs[RequestEmailChangeResult]
                response shouldBe result
              }

              verify(passportClient).requestEmailChangeCode(eq(user), eq(params))(?)
            }
      }
    }
  }

  "change email" should {
    "work" in {
      forAll(PrivateUserRefGen, PassportChangeEmailParamsGen, PassportAddIdentityResultGen) { (user, params, result) =>
        when(passportClient.getUserEssentials(eq(user), ?)(?))
          .thenReturnF(ModelGenerators.UserEssentialsGen.next)
        when(passportClient.changeEmail(?, ?)(?))
          .thenReturnF(result)

        Post(s"/1.0/user/email/change", params) ~>
          addHeader("x-uid", user.uid.toString) ~>
          commonHeaders ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe StatusCodes.OK
              contentType shouldBe ContentTypes.`application/json`

              val response = responseAs[AddIdentityResponse]
              response shouldBe result.asAutoru
            }

            verify(passportClient).changeEmail(eq(user), eq(params))(?)
          }
      }
    }
  }

}
