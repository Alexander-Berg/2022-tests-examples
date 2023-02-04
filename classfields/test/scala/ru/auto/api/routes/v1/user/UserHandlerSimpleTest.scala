package ru.auto.api.routes.v1.user

import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.Accept
import org.mockito.Mockito.reset
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.UserInfoResponse
import ru.auto.api.managers.user.UserManager
import ru.auto.api.model.CategorySelector
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients

// Unlike UserHandlerTest, this fully replaces the implementation with mocks.
class UserHandlerSimpleTest extends ApiSpec with MockedClients with BeforeAndAfter {

  override lazy val userManager = mock[UserManager]

  after(reset(userManager))

  private val commonHeaders =
    xAuthorizationHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  "get user info" should {
    "work as expected without query arguments" in {
      val user = PrivateUserRefGen.next
      val response = UserInfoResponseGen.next

      val encryptedUser = cryptoUserId.encrypt(user)

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(userManager.getUserInfo(eq(user), eq(Set.empty), countInactiveOffers = eq(false))(?)).thenReturnF(response)

      Get(
        s"/1.0/user/$encryptedUser/info"
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[UserInfoResponse] shouldBe response
        }
    }

    "work as expected with query arguments" in {
      val user = PrivateUserRefGen.next
      val response = UserInfoResponseGen.next
      val categories = Gen.nonEmptyContainerOf[Set, CategorySelector.StrictCategory](StrictCategoryGen).next

      val encryptedUser = cryptoUserId.encrypt(user)

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(userManager.getUserInfo(eq(user), eq(categories), countInactiveOffers = eq(true))(?)).thenReturnF(response)

      val query = Uri.Query.newBuilder
      query += "count_inactive_offers" -> "true"
      query ++= categories.map(c => "category" -> c.enum.name)

      Get(
        Uri(
          s"/1.0/user/$encryptedUser/info"
        ).withQuery(query.result())
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[UserInfoResponse] shouldBe response
        }
    }
  }
}
