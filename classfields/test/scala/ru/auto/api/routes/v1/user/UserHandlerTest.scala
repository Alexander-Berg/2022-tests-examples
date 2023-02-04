package ru.auto.api.routes.v1.user

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import org.mockito.Mockito.{reset, verify}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.{DealerAccountResponse, Filters}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.{AutoruUser, CategorySelector, DealerUserRoles, SortingByField}
import ru.auto.api.services.{MockedClients, MockedOffersManager}
import ru.auto.api.services.cabinet.CheckAccessView
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.util.Protobuf
import ru.yandex.passport.model.api.ApiModel.{LoadUserHint, UserResult}

import scala.util.control.NoStackTrace

/**
  *
  * @author zvez
  */
class UserHandlerTest
  extends ApiSpec
  with MockedClients
  with ScalaCheckPropertyChecks
  with BeforeAndAfter
  with MockedOffersManager {

  private val commonHeaders =
    xAuthorizationHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  private val checkAccessClientView =
    CheckAccessView(role = DealerUserRoles.Client)

  after(reset(bankerClient, cabinetClient, promocoderClient))

  // NB: we don't care much about enrichments in these tests, they're heavily tested in UserManagerSpec
  // we just will check, that there is an attempt to enrich user

  "get current user" should {

    "work for user" in {
      val result = passportUserResultWithAutoruExpertGen().next
      val userId = AutoruUser(result.getUser.getId.toLong)

      when(passportClient.getUserEssentials(eq(userId), ?)(?))
        .thenReturnF(UserEssentialsGen.next)
      when(passportClient.getUser(eq(userId))(?)).thenReturnF(result)
      when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenThrowF(new Exception)
      when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenThrowF(new Exception)
      when(promocoderClient.getFeatures(?, ?)(?)).thenThrowF(new Exception)

      Get(s"/1.0/user") ~>
        addHeader("x-uid", userId.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[UserResult](response)
            responseParsed shouldBe result
            verify(bankerClient).getPaymentMethods(?, ?, ?, ?)(?)
            verify(bankerClient).getAccountInfo(?, ?, ?)(?)
            verify(promocoderClient).getFeatures(?, ?)(?)
          }
        }
    }

    "work when with_auth_types is specified" in {
      val result = passportUserResultWithAutoruExpertGen().next
      val userId = AutoruUser(result.getUser.getId.toLong)

      when(passportClient.getUserEssentials(eq(userId), ?)(?))
        .thenReturnF(UserEssentialsGen.next)
      when(passportClient.getUserWithHints(eq(userId), eq(Seq(LoadUserHint.AUTH_TYPES)))(?)).thenReturnF(result)
      when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenThrowF(new Exception)
      when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenThrowF(new Exception)
      when(promocoderClient.getFeatures(?, ?)(?)).thenThrowF(new Exception)

      Get(s"/1.0/user?with_auth_types=true") ~>
        addHeader("x-uid", userId.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[UserResult](response)
            responseParsed shouldBe result
            verify(bankerClient).getPaymentMethods(?, ?, ?, ?)(?)
            verify(bankerClient).getAccountInfo(?, ?, ?)(?)
            verify(promocoderClient).getFeatures(?, ?)(?)
          }
        }
    }

    "work for dealer" in {
      val dealer = DealerUserRefGen.next
      val result = {
        val b = passportUserResultWithAutoruExpertGen().next.toBuilder
        b.getUserBuilder.getProfileBuilder.getAutoruBuilder
          .setClientId(dealer.clientId.toString)
          .getAutoruExpertStatusBuilder
          .setCanAdd(true)
          .setCanRead(true)
        b.build()
      }
      val userId = AutoruUser(result.getUser.getId.toLong)

      when(passportClient.getUserEssentials(eq(userId), ?)(?))
        .thenReturnF(DealerUserEssentialsGen.next)
      when(passportClient.getUser(eq(userId))(?)).thenReturnF(result)
      when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)
      when(cabinetApiClient.getDealerAccount(?, ?)(?)).thenReturnF(DealerAccountResponse.getDefaultInstance)

      Get(s"/1.0/user") ~>
        addHeader("x-uid", userId.uid.toString) ~>
        addHeader("x-dealer-id", dealer.clientId.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[UserResult](response)
            responseParsed shouldBe result
          }
        }
    }
  }

  "get user on moderation request" should {

    "work for user" in {
      val result = passportUserResultWithAutoruExpertGen().next
      val userId = AutoruUser(result.getUser.getId.toLong)

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(passportClient.getUser(eq(userId))(?)).thenReturnF(result)
      when(passportClient.getUserProfile(eq(userId))(?)).thenReturnF(PassportAutoruProfileGen.next)
      when(bankerClient.getPaymentMethods(?, ?, ?, ?)(?)).thenThrowF(new Exception)
      when(bankerClient.getAccountInfo(?, ?, ?)(?)).thenThrowF(new Exception)
      when(promocoderClient.getFeatures(?, ?)(?)).thenThrowF(new Exception)

      Get(s"/1.0/user/${userId.uid.toString}") ~>
        TokenServiceImpl.moderation.asHeader ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[UserResult](response)
            val expected = result.toBuilder
            expected.getUserBuilder
              .clearActive()
              .clearEmails()
              .clearPhones()
              .clearSocialProfiles()
              .getProfileBuilder
              .getAutoruBuilder
              .clearClientId()
            responseParsed shouldBe expected.build()
            verify(bankerClient).getPaymentMethods(?, ?, ?, ?)(?)
            verify(bankerClient).getAccountInfo(?, ?, ?)(?)
            verify(promocoderClient).getFeatures(?, ?)(?)
          }
        }
    }

    "work for dealer" in {
      val dealer = DealerUserRefGen.next
      val result = passportUserResultWithAutoruExpertGen().next
      val userId = AutoruUser(result.getUser.getId.toLong)
      val dealerProfile =
        PassportAutoruProfileGen.next.toBuilder
          .setClientId(dealer.clientId.toString)
          .build()
      when(passportClient.getUser(eq(userId))(?)).thenReturnF(result)
      when(passportClient.getUserProfile(eq(userId))(?)).thenReturnF(dealerProfile)
      when(cabinetApiClient.getDealerAccount(?, ?)(?)).thenThrowF(new Exception("fake exception") with NoStackTrace)

      Get(s"/1.0/user/${userId.uid.toString}") ~>
        TokenServiceImpl.moderation.asHeader ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[UserResult](response)
            val expected = result.toBuilder
            expected.getUserBuilder
              .clearActive()
              .clearEmails()
              .clearPhones()
              .clearSocialProfiles()
              .getProfileBuilder
              .getAutoruBuilder
              .clearClientId()
            responseParsed shouldBe expected.build()
          }
        }
    }
  }

  "get user on anon request" should {

    "work for user" in {
      val result = passportUserResultWithAutoruExpertGen().next
      val userId = AutoruUser(result.getUser.getId.toLong)

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(passportClient.getUser(eq(userId))(?)).thenReturnF(result)
      when(passportClient.getUserProfile(eq(userId))(?)).thenReturnF(PassportAutoruProfileGen.next)

      Get(s"/1.0/user/${userId.uid.toString}") ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[UserResult](response)
            val expected = result.toBuilder
            expected.getUserBuilder
              .clearActive()
              .clearEmails()
              .clearPhones()
              .clearSocialProfiles()
              .getProfileBuilder
              .getAutoruBuilder
              .clearClientId()
            responseParsed shouldBe expected.build()
          }
        }
    }

    "work for dealer" in {
      val dealer = DealerUserRefGen.next
      val result = passportUserResultWithAutoruExpertGen().next
      val userId = AutoruUser(result.getUser.getId.toLong)
      val dealerProfile =
        PassportAutoruProfileGen.next.toBuilder
          .setClientId(dealer.clientId.toString)
          .build()
      when(passportClient.getUser(eq(userId))(?)).thenReturnF(result)
      when(passportClient.getUserProfile(eq(userId))(?)).thenReturnF(dealerProfile)

      Get(s"/1.0/user/${userId.uid.toString}") ~>
        commonHeaders ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[UserResult](response)
            val expected = result.toBuilder
            expected.getUserBuilder
              .clearActive()
              .clearEmails()
              .clearPhones()
              .clearSocialProfiles()
              .getProfileBuilder
              .getAutoruBuilder
              .clearClientId()
            responseParsed shouldBe expected.build()
          }
        }
    }
  }

  "get listing" should {
    "work as expected" in {
      val paging = PagingGen.next
      val user = PrivateUserRefGen.next
      val offers = listingResponseGen(offerGen(user)).next
      val result = passportUserResultWithAutoruExpertGen().next

      // We check the actual parameter parsing elsewhere, here it should be enough to check that we actually try to parse something.
      val sorting = SortingByField("year", false)
      val motoCategory = Gen.identifier.next
      val filters = Filters.newBuilder().addMotoCategory(motoCategory).build

      val encryptedUser = cryptoUserId.encrypt(user)

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(passportClient.getUser(eq(user))(?)).thenReturnF(result)
      when(passportClient.getUserProfile(eq(user))(?)).thenReturnF(PassportAutoruProfileGen.next)
      when(offersManager.getListingForOtherUser(?, ?, ?, ?, ?)(?))
        .thenReturnF(offers)

      Get(
        s"/1.0/user/$encryptedUser/offers/all?page=${paging.page}&page_size=${paging.pageSize}&sort=year-ASC&moto_category=$motoCategory"
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK

          verify(offersManager).getListingForOtherUser(
            eq(CategorySelector.All),
            eq(user),
            eq(paging),
            eq(filters),
            eq(sorting)
          )(?)
        }
    }
  }

  "forget current user" should {
    "work as expected" in {
      val userId = PrivateUserRefGen.next
      val req = RequestGen.next

      when(passportClient.getUserEssentials(eq(userId), ?)(?))
        .thenReturnF(UserEssentialsGen.next)
      when(passportClient.forgetUser(?)(?)).thenReturnF(())

      Post(s"/1.0/user/forget") ~>
        addHeader("x-uid", userId.uid.toString) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(passportClient).forgetUser(eq(userId))(?)
        }
    }
  }
}
