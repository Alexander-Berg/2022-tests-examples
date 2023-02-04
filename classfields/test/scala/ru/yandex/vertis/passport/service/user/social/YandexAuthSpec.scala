package ru.yandex.vertis.passport.service.user.social

import org.mockito.ArgumentMatchers.{eq => eeq}
import org.scalatest.{OptionValues, WordSpec}
import ru.yandex.mds.DummyMdsClientImpl
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.FullUserDao
import ru.yandex.vertis.passport.dao.FullUserDao.FindBy
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryPerSessionStorage
import ru.yandex.vertis.passport.integration.features.{FeatureManager, FeatureRegistryFactory}
import ru.yandex.vertis.passport.model
import ru.yandex.vertis.passport.model.{FullUser, Identity, RequestContext, SocialProviders, SocializedUser, UserEmail, YandexSessionCredentials, YandexTokenCredentials}
import ru.yandex.vertis.passport.service.ban.InMemoryBanService
import ru.yandex.vertis.passport.service.user.DummySocialProviderServiceYandex
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.AuthenticationException
import ru.yandex.vertis.passport.util.crypt.{DummySigner, Signer}

import scala.concurrent.Future

class YandexAuthSpec extends WordSpec with SpecBase with MockitoSupport with OptionValues {

  import scala.concurrent.ExecutionContext.Implicits.global

  val banService = new InMemoryBanService
  val signer: Signer = DummySigner

  private val staffEmailStr = "staff@yandex-team.ru"

  private trait FixtureParam {
    val yaUserId = ModelGenerators.readableString.suchThat(_.nonEmpty).next
    val socialProviderYandex = new DummySocialProviderServiceYandex(SocialProviders.Yandex, true, Some(yaUserId))

    val socialProviderYandexStaff =
      new DummySocialProviderServiceYandex(SocialProviders.Yandex, true, Some(yaUserId), true)

    val socialProviders = Seq(
      socialProviderYandex
    )

    val socialProvidersStaff = Seq(
      socialProviderYandexStaff
    )

    val linkDecider: SocialLinkDecider = mock[SocialLinkDecider]
    val userDao: FullUserDao = mock[FullUserDao]

    val socialUserService = {
      new SocialUserServiceImpl(
        userDao,
        socialProviders,
        linkDecider,
        banService,
        new InMemoryPerSessionStorage,
        new DummyMdsClientImpl,
        signer,
        new FeatureManager(FeatureRegistryFactory.inMemory())
      )

    }
  }

  private trait FixtureParamStaff extends FixtureParam {

    override val socialUserService = {
      new SocialUserServiceImpl(
        userDao,
        socialProvidersStaff,
        linkDecider,
        banService,
        new InMemoryPerSessionStorage,
        new DummyMdsClientImpl,
        signer,
        new FeatureManager(FeatureRegistryFactory.inMemory())
      )

    }
  }

  "getYandexUserFromYandexSession" should {

    "bound user and login when yandex session equals autoru session" in new FixtureParam {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next
      val fullUser = ModelGenerators.fullUserWithAllCredentials.next

      val session = ModelGenerators.userSession.next.copy(userId = Some(fullUser.id))

      when(userDao.find(eeq(FindBy.SocialProfile(SocialProviders.Yandex, yaUserId)))(?))
        .thenReturn(Future.successful(Some(fullUser)))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, Some(session), forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.Found(fullUser)
    }

    "bound user and login when yandex session equals autoru session - staff user" in new FixtureParamStaff {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next
      val bareUser = ModelGenerators.fullUserWithAllCredentials.next

      val fullUser = bareUser.copy(emails = Seq(UserEmail(staffEmailStr, confirmed = true)))

      val session = ModelGenerators.userSession.next.copy(userId = Some(fullUser.id))

      when(userDao.find(eeq(FindBy.SocialProfile(SocialProviders.Yandex, yaUserId)))(?))
        .thenReturn(Future.successful(Some(fullUser)))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, Some(session), forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.Found(fullUser)
    }

    "bound user and login when yandex token equals autoru session" in new FixtureParam {
      val yandexTokenCreds = YandexTokenCredentials(ModelGenerators.readableString.next)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next
      val fullUser = ModelGenerators.fullUserWithAllCredentials.next

      val session = ModelGenerators.userSession.next.copy(userId = Some(fullUser.id))

      when(userDao.find(eeq(FindBy.SocialProfile(SocialProviders.Yandex, yaUserId)))(?))
        .thenReturn(Future.successful(Some(fullUser)))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexTokenCreds, Some(session), forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.Found(fullUser)
    }

    "throw when yandex session not equals autoru session" in new FixtureParam {

      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next
      val session = ModelGenerators.userSession.next
      val fullUser = ModelGenerators.fullUserWithAllCredentials.next
      when(userDao.find(eeq(FindBy.SocialProfile(SocialProviders.Yandex, yaUserId)))(?))
        .thenReturn(Future.successful(Some(fullUser)))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, Some(session), forceLogin = false)(ctx)
        .failed
        .futureValue shouldBe an[AuthenticationException]
    }

    "throw when yandex token not equals autoru session" in new FixtureParam {

      val yandexTokenCreds = YandexTokenCredentials(ModelGenerators.readableString.next)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next
      val session = ModelGenerators.userSession.next
      val fullUser = ModelGenerators.fullUserWithAllCredentials.next
      when(userDao.find(eeq(FindBy.SocialProfile(SocialProviders.Yandex, yaUserId)))(?))
        .thenReturn(Future.successful(Some(fullUser)))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      socialUserService
        .getYandexUserFromYandexCredentials(yandexTokenCreds, Some(session), forceLogin = false)(ctx)
        .failed
        .futureValue shouldBe an[AuthenticationException]
    }

    "login with session when no auto.ru session provided and bound user exists" in new FixtureParam {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val fullUser = ModelGenerators.fullUserWithAllCredentials.next

      when(userDao.find(eeq(FindBy.SocialProfile(SocialProviders.Yandex, yaUserId)))(?))
        .thenReturn(Future.successful(Some(fullUser)))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, None, forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.Found(fullUser)

    }

    "login with session when no auto.ru session provided and bound user exists - staff user" in new FixtureParam {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val bareUser = ModelGenerators.fullUserWithAllCredentials.next
      val fullUser = bareUser.copy(emails = Seq(UserEmail(staffEmailStr, confirmed = true)))

      when(userDao.find(eeq(FindBy.SocialProfile(SocialProviders.Yandex, yaUserId)))(?))
        .thenReturn(Future.successful(Some(fullUser)))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, None, forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.Found(fullUser)

    }

    "login with token when no auto.ru session provided and bound user exists" in new FixtureParam {
      val yandexTokenCreds = YandexTokenCredentials(ModelGenerators.readableString.next)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val fullUser = ModelGenerators.fullUserWithAllCredentials.next

      when(userDao.find(eeq(FindBy.SocialProfile(SocialProviders.Yandex, yaUserId)))(?))
        .thenReturn(Future.successful(Some(fullUser)))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexTokenCreds, None, forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.Found(fullUser)

    }

    "find users if they exists with same identities and return Uninked - session, forceLogin=true, multiple users found" in new FixtureParam {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val fullUser = ModelGenerators.fullUserWithAllCredentials.next
      val fullUser2 = ModelGenerators.fullUserWithAllCredentials.next

      when(userDao.find(?)(?))
        .thenAnswer { invocation =>
          invocation.getArgument[FindBy](0) match {
            case _: FindBy.SocialProfile => Future.successful(None)
            case _: FindBy.Emails => Future.successful(fullUser)
            case _ => Future.successful(Some(fullUser2))
          }
        }
      when(linkDecider.allowLink(eeq(fullUser.id), eeq(true), ?)(?)).thenReturn(Future.successful(true))
      when(linkDecider.allowLink(eeq(fullUser2.id), eeq(true), ?)(?)).thenReturn(Future.successful(true))
      when(linkDecider.isStaffAccount(eeq(fullUser.id))(?)).thenReturn(Future.successful(false))
      when(linkDecider.isStaffAccount(eeq(fullUser2.id))(?)).thenReturn(Future.successful(false))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(linkDecider.allowLogin(eeq(fullUser2), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      val phoneOwnersMap =
        fullUser2.phones.map(up => up.phone -> fullUser2.id) ++ fullUser.phones.map(up => up.phone -> fullUser.id)
      when(userDao.findPhonesOwners(?)(?)).thenReturn(Future.successful(phoneOwnersMap.toMap))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser2.id))(?)(?))
        .thenReturn(Future.successful((fullUser2, fullUser2)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, None, forceLogin = true)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe a[SocializedUser.UnLinked]
    }

    "find users if they exists with same identities and return Linked - session, forceLogin=true" in new FixtureParam {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val fullUser = ModelGenerators.fullUserWithAllCredentials.next

      when(userDao.find(?)(?))
        .thenAnswer { invocation =>
          invocation.getArgument[FindBy](0) match {
            case _: FindBy.SocialProfile => Future.successful(None)
            case _ => Future.successful(Some(fullUser))
          }
        }
      when(linkDecider.allowLink(eeq(fullUser.id), eeq(true), ?)(?)).thenReturn(Future.successful(true))
      when(linkDecider.isStaffAccount(eeq(fullUser.id))(?)).thenReturn(Future.successful(false))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, None, forceLogin = true)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe a[SocializedUser.Linked]
    }

    "find users if they exists with same identities and return list of them if no one bound - session" in new FixtureParam {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val fullUser = ModelGenerators.fullUserWithAllCredentials.next

      when(userDao.find(?)(?))
        .thenAnswer { invocation =>
          invocation.getArgument[FindBy](0) match {
            case _: FindBy.SocialProfile => Future.successful(None)
            case _ => Future.successful(Some(fullUser))
          }
        }
      when(linkDecider.allowLink(eeq(fullUser.id), eeq(true), ?)(?)).thenReturn(Future.successful(true))
      when(linkDecider.isStaffAccount(eeq(fullUser.id))(?)).thenReturn(Future.successful(false))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, None, forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.UnLinked(Seq(fullUser))
    }

    "find users if they exists with same identities and return list of them if no one bound - session && staff" in new FixtureParamStaff {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val bareUser = ModelGenerators.fullUserWithAllCredentials.next
      val fullUser = bareUser.copy(emails = Seq(UserEmail(staffEmailStr, confirmed = true)))

      when(userDao.find(?)(?))
        .thenAnswer { invocation =>
          invocation.getArgument[FindBy](0) match {
            case _: FindBy.SocialProfile => Future.successful(None)
            case _ => Future.successful(Some(fullUser))
          }
        }
      when(linkDecider.allowLink(eeq(fullUser.id), eeq(true), ?)(?)).thenReturn(Future.successful(true))
      when(linkDecider.isStaffAccount(eeq(fullUser.id))(?)).thenReturn(Future.successful(true))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, None, forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.UnLinked(Seq(fullUser))

    }

    "find users if they exists with same identities and return list of them if no one bound - token" in new FixtureParam {
      val yandexTokenCreds = YandexTokenCredentials(ModelGenerators.readableString.next)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val fullUser = ModelGenerators.fullUserWithAllCredentials.next

      when(userDao.find(?)(?))
        .thenAnswer { invocation =>
          invocation.getArgument[FindBy](0) match {
            case _: FindBy.SocialProfile => Future.successful(None)
            case _ => Future.successful(Some(fullUser))
          }
        }
      when(linkDecider.allowLink(eeq(fullUser.id), eeq(true), ?)(?)).thenReturn(Future.successful(true))
      when(linkDecider.isStaffAccount(eeq(fullUser.id))(?)).thenReturn(Future.successful(false))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?))
        .thenReturn(Future.successful(fullUser.phones.map(up => up.phone -> fullUser.id).toMap))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(Some(fullUser.id)))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexTokenCreds, None, forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.UnLinked(Seq(fullUser))

    }

    "create new user and log him in if no bound user found - session" in new FixtureParam {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val fullUser = ModelGenerators.fullUserWithAllCredentials.next

      when(userDao.find(?)(?)).thenReturn(Future.successful(None))
      when(linkDecider.allowLink(eeq(fullUser.id), eeq(true), ?)(?)).thenReturn(Future.successful(true))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), ?)(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?)).thenReturn(Future.successful(Map[model.Phone, model.UserId]()))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(None))
      when(userDao.create(?)(?)).thenReturn(Future.successful(fullUser))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, None, forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.Created(fullUser)

    }

    "if staff login, create new user and log him in if no bound user found - session" in new FixtureParamStaff {
      val yandexSessionId = ModelGenerators.readableString.next
      val yandexSslSessionId = Some(yandexSessionId)
      val yandexSessionCreds = YandexSessionCredentials(yandexSessionId, yandexSslSessionId)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val bareUser = ModelGenerators.fullUserWithAllCredentials.next
      val fullUser = bareUser.copy(emails = Seq(UserEmail(staffEmailStr, confirmed = true)))

      when(userDao.find(?)(?)).thenReturn(Future.successful(None))
      when(linkDecider.allowLink(eeq(fullUser.id), eeq(true), eeq(true))(?))
        .thenReturn(Future.successful(true))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), eeq(false))(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?)).thenReturn(Future.successful(Map[model.Phone, model.UserId]()))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(None))
      when(userDao.create(?)(?)).thenReturn(Future.successful(fullUser))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexSessionCreds, None, forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.Created(fullUser)

    }

    "create new user and log him in if no bound user found - token" in new FixtureParam {
      val yandexTokenCreds = YandexTokenCredentials(ModelGenerators.readableString.next)
      implicit val ctx: RequestContext = ModelGenerators.requestContext.next

      val fullUser = ModelGenerators.fullUserWithAllCredentials.next

      when(userDao.find(?)(?)).thenReturn(Future.successful(None))
      when(linkDecider.allowLink(eeq(fullUser.id), eeq(true), eeq(true))(?))
        .thenReturn(Future.successful(true))
      when(linkDecider.allowLogin(eeq(fullUser), eeq(SocialProviders.Yandex), eeq(true), eeq(false))(?))
        .thenReturn(Future.successful(true))
      when(userDao.findPhonesOwners(?)(?)).thenReturn(Future.successful(Map[model.Phone, model.UserId]()))
      when(userDao.findId(?)(?)).thenReturn(Future.successful(None))
      when(userDao.create(?)(?)).thenReturn(Future.successful(fullUser))
      when(userDao.updateWithPayload[FullUser](eeq(fullUser.id))(?)(?))
        .thenReturn(Future.successful((fullUser, fullUser)))

      val socializedUser = socialUserService
        .getYandexUserFromYandexCredentials(yandexTokenCreds, None, forceLogin = false)(ctx)
        .futureValue
      socializedUser.socializedUser shouldBe SocializedUser.Created(fullUser)

    }
  }

}
