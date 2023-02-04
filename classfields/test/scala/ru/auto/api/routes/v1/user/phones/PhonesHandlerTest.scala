package ru.auto.api.routes.v1.user.phones

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import org.mockito.Mockito.verify
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSuite
import ru.auto.api.ResponseModel.AddIdentityResponse
import ru.auto.api.managers.passport.PassportModelConverters.AutoruConvertable
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf
import ru.yandex.passport.model.api.ApiModel.{AddPhoneParameters, ConfirmIdentityParameters, ConfirmIdentityResult}

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class PhonesHandlerTest extends ApiSuite with ScalaCheckPropertyChecks with MockedClients {

  private val commonHeaders =
    xAuthorizationHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  test("add phone") {
    forAll(PrivateUserRefGen, PhoneGen, PassportAddIdentityResultGen) { (user, phone, result) =>
      when(passportClient.getUserEssentials(eq(user), ?)(?))
        .thenReturnF(ModelGenerators.UserEssentialsGen.next)
      when(passportClient.addPhone(?, ?)(?))
        .thenReturnF(result)

      Post(s"/1.0/user/phones?phone=$phone") ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[AddIdentityResponse](response)
            responseParsed shouldBe result.asAutoru
          }

          val expectedParams = AddPhoneParameters.newBuilder().setPhone(phone).setSteal(true).build()
          verify(passportClient).addPhone(eq(user), eq(expectedParams))(?)
        }
    }
  }

  test("add confirmed phone") {
    forAll(PrivateUserRefGen, PhoneGen, PassportAddIdentityResultGen) { (user, phone, result) =>
      when(passportClient.getUserEssentials(eq(user), ?)(?))
        .thenReturnF(ModelGenerators.UserEssentialsGen.next)
      when(passportClient.addPhone(?, ?)(?))
        .thenReturnF(result)

      Post(s"/1.0/user/phones?phone=$phone&confirmed=true") ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[AddIdentityResponse](response)
            responseParsed shouldBe result.asAutoru
          }
          val expectedParams = AddPhoneParameters.newBuilder().setPhone(phone).setConfirmed(true).build()
          verify(passportClient).addPhone(eq(user), eq(expectedParams))(?)
        }
    }
  }

  test("add phone (params in body)") {
    forAll(PrivateUserRefGen, PhoneGen, PassportAddIdentityResultGen) { (user, phone, result) =>
      val params = AddPhoneParameters.newBuilder().setPhone(phone).build()
      when(passportClient.getUserEssentials(eq(user), ?)(?))
        .thenReturnF(ModelGenerators.UserEssentialsGen.next)
      when(passportClient.addPhone(?, ?)(?))
        .thenReturnF(result)

      Post(s"/1.0/user/phones", params) ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[AddIdentityResponse](response)
            responseParsed shouldBe result.asAutoru
          }

          val expectedParams = AddPhoneParameters.newBuilder().setPhone(phone).setSteal(true).build()
          verify(passportClient).addPhone(eq(user), eq(expectedParams))(?)
        }
    }
  }

  test("remove phone") {
    forAll(PrivateUserRefGen, PhoneGen) { (user, phone) =>
      when(passportClient.getUserEssentials(eq(user), ?)(?))
        .thenReturnF(ModelGenerators.UserEssentialsGen.next)
      when(passportClient.removePhone(?, ?)(?))
        .thenReturn(Future.unit)

      Delete(s"/1.0/user/phones/$phone") ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }

          verify(passportClient).removePhone(eq(user), eq(phone))(?)
        }
    }
  }

  test("confirm phone") {
    forAll(PrivateUserRefGen, ConfirmPhoneParamsGen, ConfirmIdentityResultGen) { (user, params, result) =>
      val expectedParams = ConfirmIdentityParameters
        .newBuilder()
        .setPhone(params.getPhone)
        .setCode(params.getCode)
        .setCreateSession(false)
        .build()

      when(passportClient.getUserEssentials(eq(user), ?)(?))
        .thenReturnF(ModelGenerators.UserEssentialsGen.next)
      when(passportClient.confirmIdentity(?)(?)).thenReturnF(result)

      Post(s"/1.0/user/phones/confirm", params) ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[ConfirmIdentityResult](response)
            responseParsed shouldBe result
          }
          verify(passportClient).confirmIdentity(eq(expectedParams))(?)
        }
    }
  }

}
