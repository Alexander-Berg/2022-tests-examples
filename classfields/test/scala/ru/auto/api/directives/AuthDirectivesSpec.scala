package ru.auto.api.directives

import org.mockito.Mockito.{reset, verify}
import org.scalacheck.Gen
import org.scalatest.OneInstancePerTest
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.auth.{Application, NonStaticApplication}
import ru.auto.api.directives.AuthDirectives.UserX
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.model.ModelGenerators.{ApiTokenGen, ApitokenPayloadGen, SessionResultGen, UserEssentialsGen}
import ru.auto.api.model.{AutoruUser, RequestParams, UserRef}
import ru.auto.api.services.MockedClients
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.auto.api.services.stories.StoryClient
import ru.auto.api.util.RequestImpl
import ru.auto.cabinet.AclResponse._
import ru.yandex.passport.model.api.ApiModel.SessionResult
import ru.yandex.vertis.tracing.Traced
import ru.auto.api.exceptions.ImpersonationException

class AuthDirectivesSpec extends ApiSpec with MockedClients with OneInstancePerTest with ScalaCheckPropertyChecks {

  override lazy val passportManager: PassportManager = mock[PassportManager]
  override lazy val cabinetApiClient: CabinetApiClient = mock[CabinetApiClient]
  override lazy val storyClient: StoryClient = mock[StoryClient]

  after {
    reset(passportManager, cabinetApiClient)
  }

  implicit val trace: Traced = Traced.empty

  implicit val request: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    req.setRequestParams(RequestParams.empty)
    req
  }

  "AuthDirectives.dealerSessionWithAccessGrants()" should {
    "patch user session with dealer access grants" in {
      val user = AutoruUser(123)
      val sessionResult = SessionResultGen.next

      val userX = UserX(user, dealerRef = None, dealerRole = None, Some(sessionResult))

      val clientGroup = 1L

      val accessGroup =
        Group
          .newBuilder()
          .setId(clientGroup)
          .addGrants {
            Resource
              .newBuilder()
              .setAlias(ResourceAlias.UNKNOWN_RESOURCE)
              .setAccess(AccessLevel.READ_WRITE)
          }
          .build()

      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some(clientGroup.toString))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(accessGroup)

      val expectedAccess =
        AccessGrants
          .newBuilder()
          .setGroup(accessGroup.toBuilder.clearGrants())
          .addAllGrants(accessGroup.getGrantsList)
          .build()

      val expectedResult =
        sessionResult.toBuilder
          .setAccess(expectedAccess)
          .build()

      AuthDirectives
        .dealerSessionWithAccessGrants(passportManager, cabinetApiClient, userX, dealerRef = None)
        .futureValue shouldBe Some(expectedResult)

      verify(passportManager).getClientGroup(eq(user))(?)
      verify(cabinetApiClient).getAccessGroup(eq(clientGroup.toString))(?)
    }
  }

  "AuthDirectives.getUserInfo()" should {
    "fetch user info by application.userId" in {
      forAll(
        ApiTokenGen(ApitokenPayloadGen(userIdGen = Gen.posNum[Long].filter(_ > 0).map(Some(_)))),
        UserEssentialsGen
      ) { (token, essentials0) =>
        val application = NonStaticApplication(token)
        val user = UserRef.user(token.getPayload.getUserId)
        val essentials = essentials0.toBuilder.setId(user.uid.toString).build()

        when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
        when(passportManager.getUserEssentials(eq(user), eq(false))(?)).thenReturnF(essentials)

        val expected = UserX(user, None, None, Some(SessionResult.newBuilder().setUser(essentials).build()))

        AuthDirectives.getUserInfo(passportManager, application, None, None).futureValue shouldBe (expected)

        verify(passportManager).getSessionFromUserTicket()(?)
        reset(passportManager)
      }
    }

    "fetch user info without application.userId" in {
      forAll(ApiTokenGen(ApitokenPayloadGen(userIdGen = Gen.const(None)))) { token =>
        val application = NonStaticApplication(token)

        when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)

        AuthDirectives.getUserInfo(passportManager, application, None, None).futureValue shouldBe (UserX(UserRef.empty))

        verify(passportManager).getSessionFromUserTicket()(?)
        reset(passportManager)
      }
    }

    "mimic user when user has rights" in {
      forAll(
        ApiTokenGen(ApitokenPayloadGen(userIdGen = Gen.posNum[Long].filter(_ > 0).map(Some(_)))),
        UserEssentialsGen,
        UserEssentialsGen
      ) { (token, essentials0, essentialsNew) =>
        val application = NonStaticApplication(token)
        val user = UserRef.user(token.getPayload.getUserId)
        val essentials = essentials0.toBuilder.setId(user.uid.toString).build()
        val sessionBuilder = SessionResultGen.next.toBuilder()
        val session = sessionBuilder
          .setGrants(sessionBuilder.getGrantsBuilder.addGrants(AuthDirectives.ImpersonationGrant).build())
          .setUser(essentials)
          .build()

        val impersonatedEssentials = essentialsNew
        val impersonatedUserId = impersonatedEssentials.getId.toLong
        val impersonatedUserRef = UserRef.user(impersonatedUserId)

        when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(Some(session))
        when(passportManager.getUserEssentials(eq(user), eq(false))(?)).thenReturnF(essentials)
        when(passportManager.getUserEssentials(eq(impersonatedUserRef), eq(false))(?))
          .thenReturnF(impersonatedEssentials)

        val expected = UserX(
          impersonatedUserRef,
          None,
          None,
          Some(session.toBuilder.setUser(impersonatedEssentials).build())
        )

        AuthDirectives
          .getUserInfo(passportManager, application, Some(impersonatedUserId), None)
          .futureValue shouldBe (expected)

        verify(passportManager).getSessionFromUserTicket()(?)
        reset(passportManager)
      }
    }

    "throw when user without rights trying to mimic" in {
      forAll(
        ApiTokenGen(ApitokenPayloadGen(userIdGen = Gen.posNum[Long].filter(_ > 0).map(Some(_)))),
        UserEssentialsGen,
        UserEssentialsGen
      ) { (token, essentials0, essentialsNew) =>
        val application = NonStaticApplication(token)
        val user = UserRef.user(token.getPayload.getUserId)
        val essentials = essentials0.toBuilder.setId(user.uid.toString).build()
        val sessionBuilder = SessionResultGen.next.toBuilder()
        val session = sessionBuilder
          .setGrants(sessionBuilder.getGrantsBuilder.clearGrants().build())
          .setUser(essentials)
          .build()

        val impersonatedEssentials = essentialsNew
        val impersonatedUserId = impersonatedEssentials.getId.toLong
        val impersonatedUserRef = UserRef.user(impersonatedUserId)

        when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(Some(session))
        when(passportManager.getUserEssentials(eq(user), eq(false))(?)).thenReturnF(essentials)
        when(passportManager.getUserEssentials(eq(impersonatedUserRef), eq(false))(?))
          .thenReturnF(impersonatedEssentials)

        val thrown =
          AuthDirectives
            .getUserInfo(passportManager, application, Some(impersonatedUserId), None)
            .value
            .get
            .toEither
            .swap
            .getOrElse(throw new NoSuchElementException("no error"))

        assert(thrown.isInstanceOf[ImpersonationException])

        verify(passportManager).getSessionFromUserTicket()(?)
        reset(passportManager)
      }
    }
  }
}
