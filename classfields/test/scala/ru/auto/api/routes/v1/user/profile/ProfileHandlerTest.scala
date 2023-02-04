package ru.auto.api.routes.v1.user.profile

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSuite
import ru.auto.api.ResponseModel.UserpicUploadUriResponse
import ru.auto.api.model.{AutoruUser, ModelGenerators}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import ru.yandex.passport.model.api.ApiModel.AutoruUserProfile

/**
  *
  * @author zvez
  */
class ProfileHandlerTest extends ApiSuite with ScalaCheckPropertyChecks with MockedClients {

  private val commonHeaders =
    xAuthorizationHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  before {
    when(passportClient.getUserEssentials(?, ?)(?))
      .thenReturnF(ModelGenerators.UserEssentialsGen.next)
  }

  test("get userpic upload uri") {
    forAll(PrivateUserRefGen, ReadableStringGen) { (user, uri) =>
      when(passportClient.getUserpicUploadUri(eq(user))(?))
        .thenReturnF(uri)

      Get(s"/1.0/user/profile/userpic-upload-uri") ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[UserpicUploadUriResponse]
          response.getUri shouldBe uri
        }
    }
  }

  test("update profile") {
    forAll(PrivateUserRefGen, PassportProfilePatchGen, PassportAutoruProfileGen) { (user, patch, profile) =>
      when(passportClient.updateUserProfile(eq(user), eq(patch))(?))
        .thenReturnF(profile)

      Post(s"/1.0/user/profile", patch) ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[AutoruUserProfile]
          response shouldBe profile
        }
    }
  }

  test("get profile") {
    forAll(PrivateUserRefGen, PassportAutoruProfileGen) { (user, profile) =>
      when(passportClient.getUserProfile(eq(user))(?)).thenReturnF(profile)

      val returned = {
        val b = profile.toBuilder
        b.getAutoruExpertStatusBuilder.setCanAdd(false).setCanRead(false)
        b.build()
      }

      Get(s"/1.0/user/profile") ~>
        addHeader("x-uid", user.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[AutoruUserProfile]
          response shouldBe returned
        }
    }
  }

  test("get profile for reseller") {
    forAll(ResellerSessionResultGen, PassportAutoruProfileGen) { (session, profile) =>
      val user = AutoruUser(session.getSession.getUserId.toLong)
      when(passportClient.getSession(?)(?)).thenReturnF(session)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(session.getUser)
      when(passportClient.getUserProfile(eq(user))(?)).thenReturnF(profile)

      val returned = {
        val b = profile.toBuilder
        b.getAutoruExpertStatusBuilder.setCanAdd(false).setCanRead(true)
        b.build()
      }

      Get(s"/1.0/user/profile") ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[AutoruUserProfile]
          response shouldBe returned
        }
    }
  }

  test("get profile for dealer") {
    forAll(DealerSessionResultGen, PassportAutoruProfileGen) { (session, profile) =>
      val user = AutoruUser(session.getSession.getUserId.toLong)
      when(passportClient.getSession(?)(?)).thenReturnF(session)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(session.getUser)
      when(passportClient.getUserProfile(eq(user))(?)).thenReturnF(profile)

      val returned = {
        val b = profile.toBuilder
        b.getAutoruExpertStatusBuilder.setCanAdd(true).setCanRead(true)
        b.build()
      }

      Get(s"/1.0/user/profile") ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[AutoruUserProfile]
          response shouldBe returned
        }
    }
  }

}
