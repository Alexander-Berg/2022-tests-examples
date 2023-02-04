package ru.yandex.vertis.passport.service.user.auth

import org.joda.time.DateTime
import org.mockito.Mockito
import org.scalacheck.Gen
import org.scalatest.WordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.FullUserDao.FindBy
import ru.yandex.vertis.passport.dao.IdentityTokenCache
import ru.yandex.vertis.passport.integration.features.FeatureManager
import ru.yandex.vertis.passport.model.LoginAntifraudDecision.Forbid
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.model.proto.UserAuthTokenFailed.AuthTokenFailReason
import ru.yandex.vertis.passport.service.AccessDeniedException
import ru.yandex.vertis.passport.service.acl.AclService
import ru.yandex.vertis.passport.service.antifraud.AntifraudService
import ru.yandex.vertis.passport.service.ban.BanService
import ru.yandex.vertis.passport.service.confirmation.ConfirmationService2
import ru.yandex.vertis.passport.service.session.UserSessionService
import ru.yandex.vertis.passport.service.tvm.UserTvmService
import ru.yandex.vertis.passport.service.user.pwd.PasswordService
import ru.yandex.vertis.passport.service.user.social.SocialUserService
import ru.yandex.vertis.passport.service.user.tokens.UserAuthTokenService
import ru.yandex.vertis.passport.service.user.{ArgonHasher, PasswordUtils, UserBackendService}
import ru.yandex.vertis.passport.test.MockFeatures.{featureOff, featureOn}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.{ClientLoginNotAllowedException, TokenAuthenticationException, _}

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class AuthenticationServiceSpec extends WordSpec with SpecBase with MockitoSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  class Context {
    val sessionService: UserSessionService = mock[UserSessionService]
    val socialService: SocialUserService = mock[SocialUserService]
    val antifraudService: AntifraudService = mock[AntifraudService]
    val aclService: AclService = mock[AclService]
    val passwordService: PasswordService = mock[PasswordService]
    val confirmationService: ConfirmationService2 = mock[ConfirmationService2]
    val banService: BanService = mock[BanService]
    val userService: UserBackendService = mock[UserBackendService]
    val tokenService: UserAuthTokenService = mock[UserAuthTokenService]
    val userTvmService: UserTvmService = mock[UserTvmService]
    val identityTokenCache: IdentityTokenCache = mock[IdentityTokenCache]
    val featureManager: FeatureManager = mock[FeatureManager]

    val authService = new AuthenticationServiceImpl(
      sessionService = sessionService,
      socialService = socialService,
      aclService = aclService,
      antifraudService = antifraudService,
      passwordService = passwordService,
      confirmationService = confirmationService,
      banService = banService,
      userService = userService,
      tokenService = tokenService,
      identityTokenCache = identityTokenCache,
      userTvmService = userTvmService,
      featureManager = featureManager
    )
  }

  def withContext(f: Context => Unit): Unit = {
    f(new Context)
  }

  "AuthenticationService.login" should {
    "work with identity" in withContext { ctx =>
      val password = ModelGenerators.readableString.next
      val md5Hash = PasswordUtils.hashLegacy(password)
      val now = DateTime.now()
      val user = ModelGenerators.legacyUser
        .filter(_.emails.nonEmpty)
        .next
        .copy(pwdHash = Some(ArgonHasher.hash(md5Hash, now)), active = true)
      val session = ModelGenerators.userSession.next.copy(userId = Some(user.id))
      val email = user.emails.head.email
      val identity = Identity.Email(email)
      val identityOrToken = IdentityOrToken.RealIdentity(identity)
      val credentials = UserCredentials(identityOrToken, password)

      when(ctx.userService.find(eq(FindBy.Email(email)))(?))
        .thenReturn(Future.successful(Some(user)))
      when(ctx.antifraudService.allowLogin(?, ?)(?))
        .thenReturn(Future.successful(LoginAntifraudDecision.Allow))
      when(ctx.sessionService.createBareSession(eq(UserSessionSource2(user)))(?))
        .thenReturn(Future.successful(session))
      when(ctx.passwordService.allowPasswordLogin(?)(?)).thenReturn(Future.successful(true))

      when(ctx.userTvmService.createUserTicket(?, ?, ?)).thenReturn("userticket")
      when(ctx.identityTokenCache.getRealIdentity(eq(identityOrToken))(?)).thenReturn(Future.successful(Some(identity)))

      val result = ctx.authService.login(LoginParameters(credentials)).futureValue
      result.user shouldBe user
      result.session shouldBe session
    }

    "work with token" in withContext { ctx =>
      val identityOrToken = ModelGenerators.tokenIdentity.next
      val password = ModelGenerators.readableString.next
      val md5Hash = PasswordUtils.hashLegacy(password)
      val now = DateTime.now()
      val user = ModelGenerators.legacyUser
        .filter(_.emails.nonEmpty)
        .next
        .copy(pwdHash = Some(ArgonHasher.hash(md5Hash, now)), active = true)
      val session = ModelGenerators.userSession.next.copy(userId = Some(user.id))
      val email = user.emails.head.email
      val realIdentity = Identity.Email(email)
      val credentials = UserCredentials(identityOrToken, password)

      when(ctx.userService.find(eq(FindBy.Email(email)))(?))
        .thenReturn(Future.successful(Some(user)))
      when(ctx.antifraudService.allowLogin(?, ?)(?))
        .thenReturn(Future.successful(LoginAntifraudDecision.Allow))
      when(ctx.sessionService.createBareSession(eq(UserSessionSource2(user)))(?))
        .thenReturn(Future.successful(session))
      when(ctx.passwordService.allowPasswordLogin(?)(?)).thenReturn(Future.successful(true))

      when(ctx.userTvmService.createUserTicket(?, ?, ?)).thenReturn("userticket")
      when(ctx.identityTokenCache.getRealIdentity(eq(identityOrToken))(?))
        .thenReturn(Future.successful(Some(realIdentity)))

      val result = ctx.authService.login(LoginParameters(credentials)).futureValue
      result.user shouldBe user
      result.session shouldBe session
    }

    "fail if the token is not present in the cache" in withContext { ctx =>
      val identityOrToken = ModelGenerators.tokenIdentity.next
      val password = ModelGenerators.readableString.next
      val credentials = UserCredentials(identityOrToken, password)

      when(ctx.identityTokenCache.getRealIdentity(eq(identityOrToken))(?)).thenReturn(Future.successful(None))

      ctx.authService.login(LoginParameters(credentials)).failed.futureValue shouldBe an[IdentityIsMissingException]
    }

    "report failures to AntifraudService (user not found)" in withContext { ctx =>
      val (identity, loginParams) = genIdentityLoginParameters()

      when(ctx.userService.find(eq(FindBy.identity(identity)))(?))
        .thenReturn(Future.successful(None))
      when(ctx.antifraudService.reportFailedLogin(?, ?)(?))
        .thenReturn(Future.unit)
      when(ctx.passwordService.allowPasswordLogin(?)(?)).thenReturn(Future.successful(true))
      when(ctx.identityTokenCache.getRealIdentity(eq(loginParams.credentials.identity))(?))
        .thenReturn(Future.successful(Some(identity)))

      ctx.authService.login(loginParams).failed.futureValue shouldBe an[AuthenticationException]

      Mockito
        .verify(ctx.antifraudService)
        .reportFailedLogin(eq(identity), eq(None))(?)
    }

    "report failures to AntifraudService (wrong password)" in withContext { ctx =>
      val (identity, loginParams) = genIdentityLoginParameters()
      val user = ModelGenerators.legacyUser.next

      when(ctx.userService.find(eq(FindBy.identity(identity)))(?))
        .thenReturn(Future.successful(Some(user)))
      when(ctx.antifraudService.reportFailedLogin(?, ?)(?))
        .thenReturn(Future.unit)
      when(ctx.passwordService.allowPasswordLogin(?)(?)).thenReturn(Future.successful(true))
      when(ctx.identityTokenCache.getRealIdentity(eq(loginParams.credentials.identity))(?))
        .thenReturn(Future.successful(Some(identity)))

      ctx.authService.login(loginParams).failed.futureValue shouldBe an[AuthenticationException]

      Mockito
        .verify(ctx.antifraudService)
        .reportFailedLogin(eq(identity), eq(Some(user.id)))(?)
    }

    "don't allow to login when antifraud kicks" in withContext { ctx =>
      val (identity, loginParams) = genIdentityLoginParameters()
      val md5Hash = PasswordUtils.hashLegacy(loginParams.credentials.password)
      val now = DateTime.now()
      val user = ModelGenerators.legacyUser
        .filter(_.active)
        .next
        .copy(
          pwdHash = Some(ArgonHasher.hash(md5Hash, now))
        )

      when(ctx.userService.find(eq(FindBy.identity(identity)))(?))
        .thenReturn(Future.successful(Some(user)))
      when(ctx.antifraudService.allowLogin(eq(identity), eq(user.id))(?))
        .thenReturn(Future.successful(Forbid(LoginForbiddenReason.IpBlocked("123"), "test")))
      when(ctx.passwordService.allowPasswordLogin(?)(?)).thenReturn(Future.successful(true))
      when(ctx.identityTokenCache.getRealIdentity(eq(loginParams.credentials.identity))(?))
        .thenReturn(Future.successful(Some(identity)))

      ctx.authService.login(loginParams).failed.futureValue shouldBe a[LoginAntifraudException]
    }

    "force login via confirmation code" in withContext { ctx =>
      val (identity, loginParams) = genIdentityLoginParameters()
      val md5Hash = PasswordUtils.hashLegacy(loginParams.credentials.password)
      val now = DateTime.now()
      val user = ModelGenerators.legacyUser
        .filter(_.active)
        .next
        .copy(
          pwdHash = Some(ArgonHasher.hash(md5Hash, now))
        )

      when(ctx.userService.find(eq(FindBy.identity(identity)))(?))
        .thenReturn(Future.successful(Some(user)))
      when(ctx.passwordService.allowPasswordLogin(?)(?)).thenReturn(Future.successful(false))
      when(ctx.identityTokenCache.getRealIdentity(eq(loginParams.credentials.identity))(?))
        .thenReturn(Future.successful(Some(identity)))

      ctx.authService.login(loginParams).failed.futureValue shouldBe a[CodeLoginRequiredException]
    }

    "force change password with code" in withContext { ctx =>
      val password = ModelGenerators.readableString.next
      val md5Hash = PasswordUtils.hashLegacy(password)
      val now = DateTime.now()
      val user = ModelGenerators.legacyUser
        .filter(_.emails.nonEmpty)
        .next
        .copy(passwordExpired = true, pwdHash = Some(ArgonHasher.hash(md5Hash, now)))

      val email = user.emails.head.email
      val identity = Identity.Email(email)
      val credentials = UserCredentials(IdentityOrToken.RealIdentity(identity), password)
      val loginParams = LoginParameters(credentials)
      val code = ModelGenerators.readableString.next

      val expectedConfirmPayload = ConfirmPasswordReset(user.id)

      when(ctx.userService.find(eq(FindBy.Email(email)))(?))
        .thenReturn(Future.successful(Some(user)))
      when(ctx.passwordService.allowPasswordLogin(?)(?)).thenReturn(Future.successful(true))
      when(ctx.confirmationService.generateCodeConfirmation(eq(expectedConfirmPayload))(?))
        .thenReturn(Future.successful(code))
      when(ctx.identityTokenCache.getRealIdentity(eq(loginParams.credentials.identity))(?))
        .thenReturn(Future.successful(Some(identity)))

      val result = ctx.authService.login(LoginParameters(credentials)).failed.futureValue
      result shouldBe a[PasswordExpiredException]
      result.asInstanceOf[PasswordExpiredException].code shouldBe Some(code)
    }
  }

  "AuthenticationService.impersonate" should {
    "forbid impersonate same user" in withContext { ctx =>
      val user = ModelGenerators.fullUser.next
      val session = ModelGenerators.userSession.next.copy(userId = Some(user.id))

      when(ctx.sessionService.getBareSession(eq(session.id))(?))
        .thenReturn(Future.successful(session))

      ctx.authService.impersonate(session.id, user.id).failed.futureValue shouldBe an[AccessDeniedException]
    }

    "forbid if user is not allowed to impersonate" in withContext { ctx =>
      val user = ModelGenerators.fullUser.next
      val otherUser = ModelGenerators.fullUser.next
      val session = ModelGenerators.userSession.next.copy(userId = Some(user.id))

      when(ctx.sessionService.getBareSession(eq(session.id))(?))
        .thenReturn(Future.successful(session))
      when(ctx.aclService.canImpersonate(eq(user.id))(?))
        .thenReturn(Future.successful(false))

      ctx.authService.impersonate(session.id, otherUser.id).failed.futureValue shouldBe an[AccessDeniedException]
    }

    "forbid if target user is not allowed to be impersonated" in withContext { ctx =>
      val user = ModelGenerators.fullUser.next
      val otherUser = ModelGenerators.fullUser.next
      val session = ModelGenerators.userSession.next.copy(userId = Some(user.id))

      when(ctx.sessionService.getBareSession(eq(session.id))(?))
        .thenReturn(Future.successful(session))
      when(ctx.aclService.canImpersonate(eq(user.id))(?))
        .thenReturn(Future.successful(true))
      when(ctx.aclService.canBeImpersonated(eq(otherUser.id))(?))
        .thenReturn(Future.successful(false))

      ctx.authService.impersonate(session.id, otherUser.id).failed.futureValue shouldBe an[AccessDeniedException]
    }

    "success case" in withContext { ctx =>
      val user = ModelGenerators.fullUser.next
      val otherUser = ModelGenerators.fullUser.next
      val session = ModelGenerators.userSession.next.copy(userId = Some(user.id))
      val newSession = ModelGenerators.userSession.next.copy(userId = Some(otherUser.id))

      when(ctx.sessionService.getBareSession(eq(session.id))(?))
        .thenReturn(Future.successful(session))
      when(ctx.aclService.canImpersonate(eq(user.id))(?))
        .thenReturn(Future.successful(true))
      when(ctx.aclService.canBeImpersonated(eq(otherUser.id))(?))
        .thenReturn(Future.successful(true))
      when(ctx.userService.get(eq(otherUser.id))(?))
        .thenReturn(Future.successful(otherUser))
      when(ctx.userService.invalidateCached(otherUser.id))
        .thenReturn(Future.unit)
      when(ctx.userTvmService.createUserTicket(?, ?, ?)).thenReturn("userticket")

      val expectedSource = UserSessionSource2(otherUser, parentSession = Some(session))
      when(ctx.sessionService.createBareSession(eq(expectedSource))(?))
        .thenReturn(Future.successful(newSession))

      ctx.authService.impersonate(session.id, otherUser.id).futureValue shouldBe LoginResult(
        otherUser,
        newSession,
        Some("userticket")
      )
    }
  }
//TODO add case for token
  "AuthenticationService.loginOrRegister" should {
    "forbid banned phones" in withContext { ctx =>
      val phone = ModelGenerators.phoneNumber.next
      val phoneIdentity = Identity.Phone(phone)
      val params = LoginOrRegisterParameters(IdentityOrToken.RealIdentity(phoneIdentity))

      when(ctx.userService.find(eq(FindBy.Phone(phone)))(?)).thenReturn(Future.successful(None))

      when(ctx.banService.checkPhoneBanned(eq(phone))(?)).thenReturn(Future.successful(true))

      when(ctx.identityTokenCache.getRealIdentity(eq(IdentityOrToken.RealIdentity(phoneIdentity)))(?))
        .thenReturn(Future.successful(Some(phoneIdentity)))

      when(ctx.featureManager.ProhibitEmailRegistration).thenReturn(featureOn)

      ctx.authService.loginOrRegister(params).failed.futureValue shouldBe an[PhoneIsBannedException]
    }

    "force password login" in withContext { ctx =>
      val identity = ModelGenerators.identity.next
      val params = LoginOrRegisterParameters(IdentityOrToken.RealIdentity(identity))
      val user = ModelGenerators.fullUser.next

      when(ctx.userService.find(eq(FindBy.identity(identity)))(?))
        .thenReturn(Future.successful(Some(user)))

      when(ctx.passwordService.allowCodeLogin(eq(user), eq(identity.identityType))(?))
        .thenReturn(Future.successful(false))

      when(ctx.identityTokenCache.getRealIdentity(eq(IdentityOrToken.RealIdentity(identity)))(?))
        .thenReturn(Future.successful(Some(identity)))

      when(ctx.featureManager.ProhibitEmailRegistration).thenReturn(featureOn)

      ctx.authService.loginOrRegister(params).failed.futureValue shouldBe an[PasswordLoginRequiredException]
    }
  }

  "AuthenticationService.loginOrRegisterInternal" should {
    "login if user already exists" in withContext { ctx =>
      val identity = ModelGenerators.identity.next
      val user = ModelGenerators.legacyUser.next
      val session = ModelGenerators.session.next

      when(ctx.userService.find(eq(FindBy.identity(identity)))(?))
        .thenReturn(Future.successful(Some(user)))
      when(ctx.sessionService.createBareSession(?)(?))
        .thenReturn(Future.successful(session))
      when(ctx.userTvmService.createUserTicket(?, ?, ?)).thenReturn("userticket")
      when(ctx.featureManager.ProhibitEmailRegistration).thenReturn(featureOn)

      val res =
        ctx.authService.loginOrRegisterInternal(InternalLoginOrRegisterParameters(identity)).futureValue
      res.session shouldBe session
    }

    "create new user if it doesn't exist" in withContext { ctx =>
      val identity = ModelGenerators.identity.next
      val user = ModelGenerators.legacyUser.next
      val session = ModelGenerators.session.next

      when(ctx.userService.find(eq(FindBy.identity(identity)))(?))
        .thenReturn(Future.successful(None))
      when(ctx.sessionService.createBareSession(?)(?))
        .thenReturn(Future.successful(session))
      when(ctx.userService.createNewUser(eq(identity))(?))
        .thenReturn(Future.successful(user))
      when(ctx.userTvmService.createUserTicket(?, ?, ?)).thenReturn("userticket")
      when(ctx.featureManager.ProhibitEmailRegistration).thenReturn(featureOn)

      val res =
        ctx.authService.loginOrRegisterInternal(InternalLoginOrRegisterParameters(identity)).futureValue
      res.session shouldBe session
    }

    "create new user if it exists, but email wasn't normalized" in withContext { ctx =>
      val identity = ModelGenerators.emailYandexNonNormalizedIdentity.next
      val user = ModelGenerators.legacyUser.next
      val session = ModelGenerators.session.next

      when(ctx.userService.find(eq(FindBy.identity(identity.normalizeIdentity)))(?))
        .thenReturn(Future.successful(None))
      when(ctx.sessionService.createBareSession(?)(?))
        .thenReturn(Future.successful(session))
      when(ctx.userService.createNewUser(identity.normalizeIdentity))
        .thenReturn(Future.successful(user))
      when(ctx.userTvmService.createUserTicket(?, ?, ?)).thenReturn("userticket")
      when(ctx.featureManager.ProhibitEmailRegistration).thenReturn(featureOn)

      val res =
        ctx.authService.loginOrRegisterInternal(InternalLoginOrRegisterParameters(identity)).futureValue
      res.session shouldBe session

      Mockito.verify(ctx.userService).find(eq(FindBy.Email(identity.normalizeIdentity.email.toString)))(?)
      Mockito.verify(ctx.userService).createNewUser(eq(identity.normalizeIdentity))(?)
    }

    "provide additional payload to store in session" in withContext { ctx =>
      val identity = ModelGenerators.identity.next
      val user = ModelGenerators.legacyUser.next
      val session = ModelGenerators.session.next
      val data = ModelGenerators.someSessionData.next

      when(ctx.userService.find(eq(FindBy.identity(identity)))(?))
        .thenReturn(Future.successful(Some(user)))
      when(
        ctx.sessionService
          .createBareSession(eq(UserSessionSource2(user, data = data)))(?)
      ).thenReturn(Future.successful(session))
      when(ctx.userTvmService.createUserTicket(?, ?, ?)).thenReturn("userticket")
      when(ctx.featureManager.ProhibitEmailRegistration).thenReturn(featureOn)

      val params = InternalLoginOrRegisterParameters(identity, options = LoginOptions(data = data))
      val res = ctx.authService.loginOrRegisterInternal(params).futureValue
      res.session shouldBe session

      Mockito
        .verify(ctx.sessionService)
        .createBareSession(eq(UserSessionSource2(user, data = data)))(?)
    }
  }

  "AuthenticationService" should {
    trait WithRequestContext extends WithCustomRequestContext {
      implicit override val requestContext: RequestContext =
        wrap(ApiPayload("test", ClientInfo(platform = Some("ios"))))
    }

    val clientPassword = ModelGenerators.readableString.next
    val clientPasswordDate = new DateTime()
    val fullUser = {
      val f = ModelGenerators.fullUser.filter(_.emails.nonEmpty).next
      val md5Hash = PasswordUtils.hash(f.hashingStrategy, clientPassword, Some(clientPasswordDate))
      val p = ModelGenerators.userProfile.next.copy(clientId = Some(ModelGenerators.readableString.next))
      val now = DateTime.now()
      f.copy(
        active = true,
        profile = p,
        pwdHash = Some(ArgonHasher.hash(md5Hash, now)),
        passwordDate = Some(clientPasswordDate)
      )
    }

    val clientEmail = fullUser.emails.head.email
    val clientIdentity = Identity.Email(clientEmail)

    val socialProvider = SocialProviders.Hsd
    val socialUserSource = ModelGenerators.socialUserSource.next.copy(emails = Seq(clientEmail))
    val socializedUser = SocializedUser.Found(fullUser)

    def prepare(ctx: Context) = {
      import ctx.{eq => _, _}

      when(userService.find(eq(FindBy.Email(clientEmail)))(?)).thenReturn(Future.successful(Some(fullUser)))
      when(
        socialService.getSocializedUser( // compiler cannot disambiguate `eq` here
          eq.apply[SocialProvider](socialProvider),
          eq.apply[SocialUserSource](socialUserSource)
        )(?)
      ).thenReturn(Future.successful(socializedUser))
      when(passwordService.allowPasswordLogin(eq(fullUser))(?))
        .thenReturn(Future.successful(true))
      when(passwordService.allowCodeLogin(eq(fullUser), ?)(?))
        .thenReturn(Future.successful(true))
      when(confirmationService.requestIdentityConfirmation(?, ?, ?, ?)(?))
        .thenReturn(Future.successful("CODE"))
      when(socialService.getLastRedirectPath()(?))
        .thenReturn(Future.successful(None))
      when(antifraudService.allowLogin(?, eq(fullUser.id))(?))
        .thenReturn(Future.successful(LoginAntifraudDecision.Allow))
      when(sessionService.createBareSession(?)(?))
        .thenReturn(Future.successful(ModelGenerators.session.next))
      when(userTvmService.createUserTicket(?, ?, ?)).thenReturn("userticket")

    }

    "not allow login for client from mobile apps by default" in new WithRequestContext {
      withContext { ctx: Context =>
        import ctx.{eq => _, _}
        import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
        prepare(ctx)

        val emailIdentity = Identity.Email(clientEmail)

        when(ctx.identityTokenCache.getRealIdentity(eeq(IdentityOrToken.RealIdentity(emailIdentity)))(?))
          .thenReturn(Future.successful(Some(emailIdentity)))

        when(ctx.featureManager.ProhibitEmailRegistration).thenReturn(featureOn)

        authService
          .login(LoginParameters(UserCredentials(IdentityOrToken.RealIdentity(clientIdentity), clientPassword)))
          .failed
          .futureValue shouldBe an[ClientLoginNotAllowedException]

        authService
          .loginOrRegister(
            LoginOrRegisterParameters(IdentityOrToken.RealIdentity(emailIdentity))
          )
          .failed
          .futureValue shouldBe an[ClientLoginNotAllowedException]

        authService
          .loginSocial(
            SocialLoginParameters(socialProvider, Right(socialUserSource))
          )
          .failed
          .futureValue shouldBe an[ClientLoginNotAllowedException]
      }
    }

    "allow login for client from mobile apps if this option is enabled" in new WithRequestContext {
      withContext { ctx: Context =>
        import ctx._
        import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
        prepare(ctx)

        val emailIdentity = Identity.Email(clientEmail)

        when(ctx.identityTokenCache.getRealIdentity(eeq(IdentityOrToken.RealIdentity(emailIdentity)))(?))
          .thenReturn(Future.successful(Some(emailIdentity)))

        when(ctx.featureManager.ProhibitEmailRegistration).thenReturn(featureOn)

        authService
          .login(
            LoginParameters(
              UserCredentials(IdentityOrToken.RealIdentity(clientIdentity), clientPassword),
              options = LoginOptions(allowClientLogin = Some(true))
            )
          )
          .futureValue

        authService
          .loginOrRegister(
            LoginOrRegisterParameters(
              IdentityOrToken.RealIdentity(emailIdentity),
              options = LoginOptions(allowClientLogin = Some(true))
            )
          )
          .futureValue

        authService
          .loginSocial(
            SocialLoginParameters(
              socialProvider,
              Right(socialUserSource),
              options = LoginOptions(allowClientLogin = Some(true))
            )
          )
          .futureValue
      }
    }
  }

  "AuthenticationService.loginByToken" should {
    trait WithRequestContext extends WithCustomRequestContext {
      implicit override val requestContext: RequestContext =
        wrap(ApiPayload("test", ClientInfo(platform = Some("ios"))))
    }

    val defaultUser = ModelGenerators.fullUser.next
    val defaultToken = ModelGenerators.userAuthToken.next.copy(userId = defaultUser.id)

    def prepare(ctx: Context, tokenO: Option[UserAuthToken] = None, userO: Option[FullUser] = None): Unit = {
      import ctx.{eq => _, _}

      val token = tokenO.getOrElse(defaultToken)
      val user = userO.getOrElse(defaultUser)
      when(tokenService.getToken(eq(token.id))(?)).thenReturn(Future.successful(Some(token)))
      when(tokenService.getToken(eq(""))(?)).thenReturn(Future.successful(None))
      when(userService.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(tokenService.useToken(eq(token.id))(?)).thenReturn(Future.unit)
      when(userTvmService.createUserTicket(?, ?, ?)).thenReturn("userticket")
      when(sessionService.createBareSession(?)(?))
        .thenReturn(Future.successful(ModelGenerators.session.next))
    }

    "login" in new WithRequestContext {
      withContext { ctx =>
        import ctx._

        prepare(ctx)

        val res =
          authService.loginByToken(LoginByTokenParameters(defaultToken.id, None, options = LoginOptions())).futureValue

        res.user shouldEqual defaultUser
        res.payload shouldEqual defaultToken.payload
      }
    }

    "do not login if token is expired" in new WithRequestContext {
      withContext { ctx =>
        import ctx._

        val token = defaultToken.copy(created = DateTime.now().minusDays(3))
        prepare(ctx, tokenO = Some(token))

        val res =
          authService.loginByToken(LoginByTokenParameters(token.id, None, options = LoginOptions())).failed.futureValue
        res shouldBe an[TokenAuthenticationException]
        res.asInstanceOf[TokenAuthenticationException].reason shouldBe AuthTokenFailReason.TOKEN_EXPIRED
      }
    }

    "do not login if token not found" in new WithRequestContext {
      withContext { ctx =>
        import ctx._

        prepare(ctx)

        val res =
          authService.loginByToken(LoginByTokenParameters("", None, options = LoginOptions())).failed.futureValue
        res shouldBe an[TokenAuthenticationException]
        res.asInstanceOf[TokenAuthenticationException].reason shouldBe AuthTokenFailReason.TOKEN_NOT_FOUND
      }
    }

    "do not login if token was used" in new WithRequestContext {
      withContext { ctx =>
        import ctx._

        val token = defaultToken.copy(used = Some(DateTime.now()))
        prepare(ctx, tokenO = Some(token))

        val res =
          authService.loginByToken(LoginByTokenParameters(token.id, None, options = LoginOptions())).failed.futureValue
        res shouldBe an[TokenAuthenticationException]
        res.asInstanceOf[TokenAuthenticationException].reason shouldBe AuthTokenFailReason.TOKEN_WAS_USED
      }
    }
  }

  private def genIdentityLoginParameters(): (Identity, LoginParameters) = {
    val resolvedLoginParams = ModelGenerators.resolvedLoginParameters.next
    val identity = resolvedLoginParams.credentials.identity
    val loginParams = resolvedLoginParams.withIdentity(IdentityOrToken.RealIdentity(identity))
    (identity, loginParams)
  }
}
