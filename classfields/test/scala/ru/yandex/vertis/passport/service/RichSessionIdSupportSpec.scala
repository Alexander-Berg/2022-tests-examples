package ru.yandex.vertis.passport.service

import akka.actor.ActorSystem
import org.joda.time.{Instant, Minutes}
import org.mockito.Mockito
import org.scalatest.{BeforeAndAfter, FreeSpec}
import ru.yandex.passport.BlackboxClient
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.passport.dao.impl.memory.{InMemorySessionDao, InMemoryUserEssentialsCache, InMemoryUserTicketCache}
import ru.yandex.vertis.passport.integration.GenUserProvider
import ru.yandex.vertis.passport.model.{JustBasicSessionData, RequestContext, Session, SessionUser, UserSessionSource}
import ru.yandex.vertis.passport.service.acl.DummyGrantsService
import ru.yandex.vertis.passport.service.identity.{DeviceUidService, LegacyIdentityService}
import ru.yandex.vertis.passport.service.impl.RichSessionIdService
import ru.yandex.vertis.passport.service.session.{RichSessionIdSupport, UserSessionServiceImpl}
import ru.yandex.vertis.passport.service.tvm.{UserTvmService, UserTvmServiceImpl}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{CouchbaseSupport, ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.InvalidSessionIdException
import ru.yandex.vertis.passport.util.crypt.HmacSigner
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport}

import scala.concurrent.Future
import scala.util.Try

/**
  * Tests for [[RichSessionIdSupport]]
  *
  * @author zvez
  */
class RichSessionIdSupportSpec
  extends FreeSpec
  with SpecBase
  with MockitoSupport
  with BeforeAndAfter
  with CouchbaseSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val Ttl = Minutes.minutes(5).toStandardDuration
  implicit lazy val actorSystem = ActorSystem(getClass.getSimpleName)
  val tracing = LocalTracingSupport(EndpointConfig.Empty)

  val userTvmService: UserTvmServiceImpl = UserTvmServiceImpl(
    new InMemoryUserTicketCache,
    tracing,
    actorSystem.scheduler,
    None
  )
  val userDao = Mockito.spy(new InMemoryUserEssentialsCache)
  val sessionDao = Mockito.spy(new InMemorySessionDao)
  val userProvider = Mockito.spy(new GenUserProvider)
  val sessionIdService = new RichSessionIdService(new HmacSigner("secret"))
  val blackboxClient = mock[BlackboxClient]

  val sessionService =
    new UserSessionServiceImpl(
      sessionDao,
      userProvider,
      sessionIdService,
      new DeviceUidService(LegacyIdentityService),
      userTvmService,
      DummyGrantsService,
      blackboxClient
    ) with RichSessionIdSupport

  "RichSessionIdSupport" - {
    "should fail fast on not valid session ids" in {
      val fakeSessionId = ModelGenerators.richSessionId.next
      sessionService.get(fakeSessionId).failed.futureValue shouldBe an[InvalidSessionIdException]
      Mockito.verifyNoMoreInteractions(sessionDao)
    }

    var session: Session = null

    "should work when backend is online" in {
      val source = UserSessionSource(ModelGenerators.userId.next, Some(Ttl), data = ModelGenerators.sessionData.next)
      val createResult = sessionService.create(source).futureValue
      session = createResult.session
      val getResult = sessionService.get(session.id).futureValue
      getResult.session shouldBe session
      getResult.trusted shouldBe true
    }

    "shouldn't try to recover when session was not found" in {
      val fakeSid = sessionIdService.generate(JustBasicSessionData(SessionUser("1"), Instant.now(), Ttl))
      sessionService.get(fakeSid).failed.futureValue shouldBe a[NoSuchElementException]
    }

    "when storage goes down we should fallback to sid and users source" in {
      when(Try(sessionDao.get(?)(?)).toOption.orNull).thenReturn(Future.failed(new Exception("down")))
      val res = sessionService.get(session.id).futureValue
      res.trusted shouldBe false
      res.session.id shouldBe session.id
      res.session.owner shouldBe session.owner
      res.session.creationDate shouldBe session.creationDate
      res.session.ttl shouldBe session.ttl
      res.user shouldBe defined
    }

    "when users source is not available as well we should return just basic session data" in {
      when(Try(userProvider.get(?, ?, ?)(?)).toOption.orNull)
        .thenReturn(Future.failed(new Exception("down")))
      val res = sessionService.get(session.id).futureValue
      res.trusted shouldBe false
      res.session.id shouldBe session.id
      res.session.owner shouldBe session.owner
      res.session.creationDate shouldBe session.creationDate
      res.session.ttl shouldBe session.ttl
      res.user shouldBe empty
    }
  }

}
