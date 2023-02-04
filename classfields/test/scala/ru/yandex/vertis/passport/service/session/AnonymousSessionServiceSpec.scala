package ru.yandex.vertis.passport.service.session

import java.util.NoSuchElementException

import akka.actor.ActorSystem
import org.joda.time.Duration
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryUserTicketCache
import ru.yandex.vertis.passport.model.{AnonymousSessionSource, Session}
import ru.yandex.vertis.passport.service.identity.{DeviceUidService, LegacyIdentityService}
import ru.yandex.vertis.passport.service.impl.RichSessionIdService
import ru.yandex.vertis.passport.service.tvm.UserTvmServiceImpl
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.InvalidSessionIdException
import ru.yandex.vertis.passport.util.crypt.HmacSigner
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport}

import scala.concurrent.duration.DurationInt

/**
  * Tests for [[AnonymousSessionServiceImpl]]
  *
  * @author zvez
  */
class AnonymousSessionServiceSpec extends WordSpec with SpecBase {

  import ru.yandex.vertis.passport.util.DateTimeUtils.toJodaDuration

  import scala.concurrent.ExecutionContext.Implicits.global

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

  val sessionService =
    new AnonymousSessionServiceImpl(sessionIdService, new DeviceUidService(LegacyIdentityService), userTvmService)

  val Uid = LegacyIdentityService.generate()
  val Ttl: Duration = 5.minutes

  "EphemeralSessionService" should {
    var createdSession: Session = null

    "create session" in {
      val source = AnonymousSessionSource(Some(Uid), Some(Ttl))
      val result = sessionService.create(source).futureValue
      result.session.deviceUid shouldBe Uid
      result.session.ttl shouldBe Ttl
      result.userTicket should be(defined)
      createdSession = result.session
    }

    "get created session" in {
      val result = sessionService.get(createdSession.id).futureValue
      result.userTicket should be(defined)
      result.session shouldBe createdSession
    }

    "discard wrong session id" in {
      val res = sessionService.get(ModelGenerators.fakeSessionId.next)
      res.failed.futureValue shouldBe a[InvalidSessionIdException]
    }

    "discard expired session id" in {
      val source = AnonymousSessionSource(Some(Uid), Some(1.second))
      val session = sessionService.create(source).futureValue.session
      sessionService.get(session.id).futureValue
      Thread.sleep(1100)
      sessionService.get(session.id).failed.futureValue shouldBe a[NoSuchElementException]
    }

    "prolong almost expired session (slow)" in {
      val ttl = 7
      val source = AnonymousSessionSource(Some(Uid), Some(ttl.seconds))
      val session = sessionService.create(source).futureValue.session
      val getResults = (1 to ttl + 1).map { _ =>
        Thread.sleep(1000)
        sessionService
          .get(session.id)
          .map(Some(_))
          .recover { case _: NoSuchElementException => None }
          .futureValue
      }

      val okResults = getResults.takeWhile(_.isDefined).flatten
      okResults.size should be > ttl / 2
      okResults.size should be < getResults.size
      okResults.takeWhile(_.newSession.isEmpty).size should be < okResults.size
      okResults.flatMap(_.newSession).distinct.size should be(1)
    }
  }

}
