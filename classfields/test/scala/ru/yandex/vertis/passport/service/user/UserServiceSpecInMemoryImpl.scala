package ru.yandex.vertis.passport.service.user

import akka.actor.ActorSystem
import ru.yandex.mds.DummyMdsClientImpl
import ru.yandex.passport.BlackboxClient
import ru.yandex.vertis.mockito.MockitoSupport.mock
import ru.yandex.vertis.passport.dao.IdentityTokenCache
import ru.yandex.vertis.passport.dao.impl.memory.{InMemoryConfirmationDao, InMemoryIdentityTokenCache, InMemoryPerSessionStorage}
import ru.yandex.vertis.passport.integration.CachedUserProviderImpl
import ru.yandex.vertis.passport.integration.features.{FeatureManager, FeatureRegistryFactory}
import ru.yandex.vertis.passport.service.acl.{DummyAclService, DummyGrantsService}
import ru.yandex.vertis.passport.service.antifraud.DummyAntifraudService
import ru.yandex.vertis.passport.service.ban.{DummyBanService, DummyUserModerationStatusProvider}
import ru.yandex.vertis.passport.service.confirmation.{ConfirmationService2, DummyConfirmationProducer}
import ru.yandex.vertis.passport.service.identity.{DeviceUidService, LegacyIdentityService}
import ru.yandex.vertis.passport.service.impl.SimpleSessionIdService
import ru.yandex.vertis.passport.service.session.UserSessionServiceImpl
import ru.yandex.vertis.passport.service.tvm.UserTvmServiceImpl
import ru.yandex.vertis.passport.service.user.auth.AuthenticationServiceImpl
import ru.yandex.vertis.passport.service.user.pwd.{PasswordPolicies, PasswordService}
import ru.yandex.vertis.passport.service.user.social.{AlwaysAllowSocialLinkDecider, SocialUserServiceImpl}
import ru.yandex.vertis.passport.service.user.tokens.{UserAuthTokensGenerator, UserAuthTokensServiceImpl}
import ru.yandex.vertis.passport.service.vox.VoxEncryptors
import ru.yandex.vertis.passport.util.crypt.{BlowfishEncryptor, DummySigner}
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport}

/**
  * [[UserServiceSpec]] implementation using in-memory storages
  *
  * @author zvez
  */
class UserServiceSpecInMemoryImpl extends UserServiceSpec with InMemoryDaoProvider with TestSocialProviders {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val actorSystem = ActorSystem(getClass.getSimpleName)

  val identityService = LegacyIdentityService
//    new IdentityServiceImpl(new HmacSigner("secret"), "localhost")
//      with LegacyIdentitySupport
  val tracing = LocalTracingSupport(EndpointConfig.Empty)

  val userTvmService: UserTvmServiceImpl = UserTvmServiceImpl(
    userTicket,
    tracing,
    actorSystem.scheduler,
    None
  )
  val userProvider = new CachedUserProviderImpl(userCache, new PassportUserProvider(userDao))

  val grantsService = DummyGrantsService

  lazy val blackboxClient = mock[BlackboxClient]

  val sessionService =
    new UserSessionServiceImpl(
      sessionDao,
      userProvider,
      SimpleSessionIdService,
      new DeviceUidService(identityService),
      userTvmService,
      grantsService,
      blackboxClient
    )

  val confirmationDao = new InMemoryConfirmationDao

  val confirmationService =
    new ConfirmationService2(confirmationDao, DummyConfirmationProducer, DummyConfirmationProducer)

  val socialService =
    new SocialUserServiceImpl(
      userDao,
      socialProviders,
      AlwaysAllowSocialLinkDecider,
      DummyBanService,
      new InMemoryPerSessionStorage,
      new DummyMdsClientImpl,
      DummySigner,
      new FeatureManager(FeatureRegistryFactory.inMemory())
    )

  val passwordPolicy = PasswordPolicies.Empty

  val passwordService =
    new PasswordService(
      passwordPolicy,
      DummyAclService,
      previousPasswordsDao,
      DummyUserModerationStatusProvider
    )

  val userBackendService = new UserBackendService(userDao, DummyUserModerationStatusProvider, sessionService)

  val userAuthTokensGen: UserAuthTokensGenerator = new UserAuthTokensGenerator {}

  val userAuthTokensService: UserAuthTokensServiceImpl = new UserAuthTokensServiceImpl(
    userProvider,
    userAuthTokensGen,
    userAuthTokenDao
  )
  val identityTokenCache: IdentityTokenCache = new InMemoryIdentityTokenCache

  val authService = new AuthenticationServiceImpl(
    sessionService,
    socialService,
    DummyAclService,
    DummyAntifraudService,
    passwordService,
    confirmationService,
    DummyBanService,
    userBackendService,
    userAuthTokensService,
    identityTokenCache,
    userTvmService = userTvmService,
    new FeatureManager(FeatureRegistryFactory.inMemory())
  )

  val voxEncryptors =
    VoxEncryptors(new BlowfishEncryptor("vox-username-test"), new BlowfishEncryptor("vox-password-test"))

  val userService = new UserServiceImpl(
    userDao,
    DummyUserModerationStatusProvider,
    sessionService,
    confirmationService,
    DummyBanService,
    passwordService,
    DummyGrantsService,
    userTvmService = userTvmService,
    voxEncryptors = voxEncryptors,
    identityTokenCache = identityTokenCache
  )
}
