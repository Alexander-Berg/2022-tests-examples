package ru.yandex.vertis.passport.service.user

import akka.actor.ActorSystem
import ru.yandex.mds.DummyMdsClientImpl
import ru.yandex.passport.BlackboxClient
import ru.yandex.vertis.mockito.MockitoSupport.mock
import ru.yandex.vertis.passport.dao.impl.memory.{InMemoryConfirmationDao, InMemoryPerSessionStorage}
import ru.yandex.vertis.passport.dao.impl.mysql.AutoruClientUsersDao
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
import ru.yandex.vertis.passport.service.user.client.ClientServiceImpl
import ru.yandex.vertis.passport.service.user.pwd.{PasswordPolicies, PasswordService}
import ru.yandex.vertis.passport.service.user.social.{AlwaysAllowSocialLinkDecider, SocialUserServiceImpl}
import ru.yandex.vertis.passport.service.user.tokens.{UserAuthTokensGenerator, UserAuthTokensServiceImpl}
import ru.yandex.vertis.passport.service.vox.VoxEncryptors
import ru.yandex.vertis.passport.test.RedisStartStopSupport
import ru.yandex.vertis.passport.util.crypt.{BlowfishEncryptor, DummySigner}
import ru.yandex.vertis.passport.util.mysql.DualDatabase
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport}

trait ServiceProviderLegacyImpl extends DbDaoProvider with TestSocialProviders {

  val identityService = LegacyIdentityService
  //    new newIdentityServiceImpl(new HmacSigner("secret"), "localhost")
  //      with LegacyIdentitySupport
  implicit lazy val actorSystem = ActorSystem(getClass.getSimpleName)

  lazy val userProvider = new CachedUserProviderImpl(userCache, new PassportUserProvider(userDao))
  lazy val tracing = LocalTracingSupport(EndpointConfig.Empty)

  lazy val userTvmService: UserTvmServiceImpl = UserTvmServiceImpl(
    userTicket,
    tracing,
    actorSystem.scheduler,
    None
  )

  lazy val blackboxClient = mock[BlackboxClient]

  lazy val sessionService =
    new UserSessionServiceImpl(
      sessionDao,
      userProvider,
      SimpleSessionIdService,
      new DeviceUidService(identityService),
      userTvmService,
      DummyGrantsService,
      blackboxClient
    )

  lazy val confirmationDao = new InMemoryConfirmationDao

  lazy val confirmationService =
    new ConfirmationService2(confirmationDao, DummyConfirmationProducer, DummyConfirmationProducer)

  lazy val socialService =
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

  lazy val passwordPolicy = PasswordPolicies.Empty

  lazy val passwordService =
    new PasswordService(
      passwordPolicy,
      DummyAclService,
      previousPasswordsDao,
      DummyUserModerationStatusProvider
    )

  lazy val userBackendService = new UserBackendService(userDao, DummyUserModerationStatusProvider, sessionService)

  lazy val userAuthTokensGen: UserAuthTokensGenerator = new UserAuthTokensGenerator {}

  lazy val userAuthTokensService: UserAuthTokensServiceImpl = new UserAuthTokensServiceImpl(
    userProvider,
    userAuthTokensGen,
    userAuthTokenDao
  )

  lazy val authService = new AuthenticationServiceImpl(
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
    userTvmService,
    new FeatureManager(FeatureRegistryFactory.inMemory())
  )

  lazy val voxEncryptors =
    VoxEncryptors(new BlowfishEncryptor("vox-username-test"), new BlowfishEncryptor("vox-password-test"))

  lazy val userService = new UserServiceImpl(
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

  lazy val officeDao = new AutoruClientUsersDao(DualDatabase(dbs.legacyOffice))

  lazy val clientService = new ClientServiceImpl(sessionService, userService, userDao, officeDao)

}
