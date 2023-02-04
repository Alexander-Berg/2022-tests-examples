package ru.yandex.vertis.passport.service.session

import akka.actor.ActorSystem
import ru.yandex.passport.BlackboxClient
import ru.yandex.vertis.mockito.MockitoSupport.mock
import ru.yandex.vertis.passport.dao.impl.couchbase.CouchbaseSessionDao
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryUserTicketCache
import ru.yandex.vertis.passport.dao.impl.redis.{RedisSessionDao, RedisUserEssentialsCache}
import ru.yandex.vertis.passport.integration.{CachedUserProviderImpl, GenUserProvider}
import ru.yandex.vertis.passport.service.acl.DummyGrantsService
import ru.yandex.vertis.passport.service.identity.{DeviceUidService, LegacyIdentityService}
import ru.yandex.vertis.passport.service.impl.RichSessionIdService
import ru.yandex.vertis.passport.service.tvm.UserTvmServiceImpl
import ru.yandex.vertis.passport.test.{CouchbaseSupport, RedisStartStopSupport, RedisSupport}
import ru.yandex.vertis.passport.util.crypt.HmacSigner
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport}

/**
  * Tests for [[UserSessionService]] bucked with couchbase dao
  *
  * @author zvez
  */
class UserSessionServiceImplSpec extends UserSessionServiceSpec with RedisSupport with RedisStartStopSupport {

  lazy val sessionDao = new RedisSessionDao(createCache("test-session"))
  lazy val userDao = new RedisUserEssentialsCache(createCache("test-user-essentials"))

  lazy val userProvider = new CachedUserProviderImpl(userDao, GenUserProvider)

  val sessionIdService = new RichSessionIdService(new HmacSigner("secret"))

  implicit lazy val actorSystem = ActorSystem(getClass.getSimpleName)
  val tracing = LocalTracingSupport(EndpointConfig.Empty)
  lazy val userTicket = new InMemoryUserTicketCache

  val userTvmService: UserTvmServiceImpl = UserTvmServiceImpl(
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
      sessionIdService,
      new DeviceUidService(LegacyIdentityService),
      userTvmService,
      DummyGrantsService,
      blackboxClient
    )
}
