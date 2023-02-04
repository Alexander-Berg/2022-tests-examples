package ru.yandex.vertis.passport.service

import akka.actor.ActorSystem
import org.joda.time.Minutes
import org.scalatest.WordSpec
import ru.yandex.passport.BlackboxClient
import ru.yandex.vertis.mockito.MockitoSupport.mock
import ru.yandex.vertis.passport.dao.impl.memory.{InMemorySessionDao, InMemoryUserEssentialsCache, InMemoryUserTicketCache}
import ru.yandex.vertis.passport.integration.{CachedUserProviderImpl, GenUserProvider}
import ru.yandex.vertis.passport.model.{ApiPayload, ClientInfo, RequestContext, UserGrantsSet, UserSessionSource}
import ru.yandex.vertis.passport.service.acl.DummyGrantsService
import ru.yandex.vertis.passport.service.identity.{DeviceUidService, LegacyIdentityService}
import ru.yandex.vertis.passport.service.impl.SimpleSessionIdService
import ru.yandex.vertis.passport.service.session.SecuredUserSessionService.ClientChangedException
import ru.yandex.vertis.passport.service.session.{SecuredUserSessionService, UserSessionServiceImpl}
import ru.yandex.vertis.passport.service.tvm.UserTvmServiceImpl
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{CouchbaseSupport, ModelGenerators, SpecBase}
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport}

/**
  * tests for [[SecuredUserSessionService]]
  *
  * @author zvez
  */
//scalastyle:off multiple.string.literals
class SecuredSessionServiceSpec extends WordSpec with SpecBase {

  import scala.concurrent.ExecutionContext.Implicits.global

  val sessionDao = new InMemorySessionDao
  val userDao = new InMemoryUserEssentialsCache
  val userService = new CachedUserProviderImpl(userDao, GenUserProvider)

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

  val sessionService =
    new UserSessionServiceImpl(
      sessionDao,
      userService,
      SimpleSessionIdService,
      new DeviceUidService(LegacyIdentityService),
      userTvmService,
      DummyGrantsService,
      blackboxClient
    ) with SecuredUserSessionService

  private val userId = ModelGenerators.userId.next
  private val ttl = Minutes.minutes(5).toStandardDuration

  "SecuredSessionServiceSpec" should {
    "work" in new WithCustomRequestContext {
      implicit val requestContext = wrap(ApiPayload("test", ClientInfo(agent = Some("something"))))
      val source = UserSessionSource(userId, Some(ttl))
      val result = sessionService.create(source).futureValue
      result.user.get.id should be(userId)
      result.session.clientHash should be(defined)
      val session = sessionService.get(result.session.id).futureValue
      val sessionWithoutJwt = session.copy(userTicket = None, grants = None)
      sessionWithoutJwt should be(result.copy(userTicket = None))
      session.grants shouldBe Some(UserGrantsSet(Nil))
      session.userTicket shouldNot be(result.userTicket)
    }

    "work when no client info specified" in new WithCustomRequestContext {
      implicit val requestContext = wrap(ApiPayload("test"))
      val source = UserSessionSource(userId, Some(ttl))
      val result = sessionService.create(source).futureValue
      result.user.get.id should be(userId)
      result.session.clientHash should be(defined)

      val session = sessionService.get(result.session.id).futureValue
      val sessionWithoutJwt = session.copy(userTicket = None, grants = None)
      sessionWithoutJwt should be(result.copy(userTicket = None))
      session.grants shouldBe Some(UserGrantsSet(Nil))
      session.userTicket shouldNot be(result.userTicket)
    }

    "reject get request and drop session when client agent changed" in {
      val ctx1 = wrap(ApiPayload("test", ClientInfo(agent = Some("something"))))
      val source = UserSessionSource(userId, Some(ttl))
      val result = sessionService.create(source)(ctx1).futureValue

      val ctx2 = wrap(ApiPayload("test", ClientInfo(agent = Some("something else"))))
      sessionService.get(result.session.id)(ctx2).failed.futureValue shouldBe a[ClientChangedException]

      sessionService.get(result.session.id)(ctx1).failed.futureValue shouldBe a[NoSuchElementException]
    }
  }

}
