package ru.auto.api.routes.v1.vox

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.{AddVoxUserResponse, VoxOttResponse}
import ru.auto.api.model.ModelGenerators.{PrivateUserRefGen, UserEssentialsGen}
import ru.auto.api.services.MockedClients
import ru.auto.api.services.vox.VoxManager
import ru.auto.api.util.Protobuf

import scala.concurrent.Future
import scala.util.Random

class VoxHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks with BeforeAndAfter {

  override lazy val voxManager: VoxManager = mock[VoxManager]

  private val commonHeaders =
    xAuthorizationHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  "sign-up-user" should {

    "work" in {
      val user = PrivateUserRefGen.next

      when(passportClient.getUserEssentials(eq(user), ?)(?))
        .thenReturnF(UserEssentialsGen.next)

      val rndUsername = Random.alphanumeric.take(30).mkString("")
      val serviceResponse = AddVoxUserResponse.newBuilder().setUsername(rndUsername).build()
      when(voxManager.signUpUser(?)).thenReturnF(serviceResponse)

      Post(s"/1.0/vox/sign-up-user") ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
            val responseParsed = Protobuf.fromJson[AddVoxUserResponse](response)
            responseParsed shouldBe serviceResponse
          }
        }
    }

  }

  "generate-ott" should {
    "generate correct token by the rules of vox" in {
      val user = PrivateUserRefGen.next

      when(passportClient.getUserEssentials(eq(user), ?)(?))
        .thenReturnF(UserEssentialsGen.next)

      val sampleOneTimeLogin = Gen.identifier.next
      val username = Gen.identifier.next
      val serviceResponse = VoxOttResponse.newBuilder().setToken(username).build()
      when(voxManager.generateOtt(eq(sampleOneTimeLogin))(?)).thenReturn(Future.successful(serviceResponse))

      Get(s"/1.0/vox/generate-ott?login-key=$sampleOneTimeLogin") ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
            val responseParsed = Protobuf.fromJson[VoxOttResponse](response)
            responseParsed shouldBe serviceResponse
          }
        }

    }
  }

}
