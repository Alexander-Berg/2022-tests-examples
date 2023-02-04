package ru.yandex.vertis.passport.service.session

import io.jsonwebtoken.Jwts
import org.joda.time.Minutes
import org.scalatest.FreeSpec
import ru.yandex.passport.model.api.ApiModel.SessionResult
import ru.yandex.vertis.passport.integration.CachedUserProvider
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.service.identity.LegacyIdentityService
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.DateTimeUtils.toJodaDuration
import ru.yandex.vertis.passport.util.SessionNotFoundException

import java.util.{Base64, NoSuchElementException}
import scala.concurrent.duration.DurationInt

/**
  * Tests for [[UserSessionServiceImpl]]
  *
  * @author zvez
  */
//scalastyle:off multiple.string.literals
trait UserSessionServiceSpec extends FreeSpec with SpecBase {

  import scala.concurrent.ExecutionContext.Implicits.global

  def sessionService: UserSessionServiceImpl
  def userProvider: CachedUserProvider

  private val userId = ModelGenerators.userId.next
  private val Ttl = Minutes.minutes(5).toStandardDuration

  "SessionService" - {
    "create session and get it" in {
      val source = UserSessionSource(userId, Some(Ttl))
      val result = sessionService.create(source).futureValue
      result.user.get.id should be(userId)
      result.session.data should be(source.data)

      val session = sessionService.get(result.session.id).futureValue
      val sessionWithoutJwt = session.copy(userTicket = None, grants = None)
      sessionWithoutJwt should be(result.copy(userTicket = None))
      session.grants shouldBe Some(UserGrantsSet(Nil))
      session.userTicket shouldNot be(result.userTicket)
    }

    "create session should fail if user doesn't exist" in {
      val source = UserSessionSource("f" + userId, Some(Ttl))
      sessionService.create(source).failed.futureValue shouldBe a[NoSuchElementException]
    }

    "prolong session on get (sloooooow)" in {
      val ttl = 5
      val source = UserSessionSource(userId, Some(ttl.seconds))
      val session = sessionService.create(source).futureValue.session
      val getResults = (1 to (ttl * 3)).map { _ =>
        Thread.sleep(1000)
        sessionService
          .get(session.id)
          .map(Some(_))
          .recover { case _: NoSuchElementException => None }
          .futureValue
      }

      val okResults = getResults.takeWhile(_.isDefined).flatten
      okResults.size should be > ttl
      okResults.size should be < getResults.size
      okResults.takeWhile(_.newSession.isEmpty).size should be < okResults.size
      val results = okResults.flatMap(_.newSession)
      results.distinct.size should be(1)
    }

    "NOT generate uid if passed-one is not valid" in {
      val wrongUid = "something.wrong"
      val ctx = wrap(ApiPayload("test", ClientInfo(deviceUid = Some(wrongUid))))
      val source = UserSessionSource(userId, Some(Ttl))
      val result = sessionService.create(source)(ctx).futureValue
      result.session.deviceUid should be(wrongUid)
    }

    "keep passed uid if it is valid" in {
      val goodUid = LegacyIdentityService.generate()
      val reqCtx = wrap(ApiPayload("test", ClientInfo(deviceUid = Some(goodUid))))
      val source = UserSessionSource(userId, Some(Ttl))
      val result = sessionService.create(source)(reqCtx).futureValue
      result.session.deviceUid shouldBe goodUid
    }

    "create impersonated session" in {
      val adminUser = ModelGenerators.fullUser.next
      val targetUser = ModelGenerators.fullUser.next
      val parentSession = {
        val source = UserSessionSource2(adminUser, Some(Ttl))
        sessionService.createBareSession(source).futureValue
      }
      val source = UserSessionSource2(targetUser, Some(Ttl), parentSession = Some(parentSession))
      val result = sessionService.createBareSession(source).futureValue

      result.userId shouldBe Some(targetUser.id)
      result.parentSession shouldBe Some(ParentSession.fromSession(parentSession))
    }

    "create session with additional payload" in {
      val user = ModelGenerators.fullUser.next
      val data = ModelGenerators.someSessionData.next
      val source = UserSessionSource2(user, Some(Ttl), data = data)
      val result = sessionService.createBareSession(source).futureValue
      result.data shouldBe data
    }

    "should drop old sessions if user create too many" in {
      val userId = ModelGenerators.userId.next
      val sessions = (1 to sessionService.maxSessionsPerUser * 2).map { _ =>
        val source = UserSessionSource(userId, Some(Ttl))
        sessionService.create(source).futureValue.session
      }

      sessions.take(sessionService.maxSessionsPerUser).foreach { session =>
        val fv = sessionService.get(session.id).failed.futureValue
        fv shouldBe a[SessionNotFoundException]
      }

      val expectedLiveSessions = sessions.drop(sessionService.maxSessionsPerUser)
      expectedLiveSessions.foreach { session =>
        sessionService.get(session.id).futureValue.session shouldBe session
      }

      val userSids = sessionService
        .getLastUserSessions(userId, sessionService.maxSessionsPerUser)
        .futureValue
      userSids shouldBe expectedLiveSessions
    }

    "should skip expired sessions before drop the oldest" in {
      val longSessions = (1 to 5).map { _ =>
        val source = UserSessionSource(userId, Some(Ttl))
        sessionService.create(source).futureValue.session
      }

      (longSessions.size until sessionService.maxSessionsPerUser).foreach { _ =>
        val source = UserSessionSource(userId, Some(3.seconds))
        sessionService.create(source).futureValue.session
      }

      Thread.sleep(6.seconds.toMillis)

      val newSessions = (1 to 5).map { _ =>
        val source = UserSessionSource(userId, Some(Ttl))
        sessionService.create(source).futureValue.session
      }

      val expectedLiveSessions = longSessions ++ newSessions

      expectedLiveSessions.foreach { session =>
        sessionService.get(session.id).futureValue.session shouldBe session
      }

      val userSids = sessionService
        .getLastUserSessions(userId, sessionService.maxSessionsPerUser)
        .futureValue
      userSids shouldBe expectedLiveSessions
    }

    "should create valid userTicket in session" in {
      val source = UserSessionSource(userId, Some(Ttl))
      val result = sessionService.create(source).futureValue
      result.user.get.id should be(userId)
      result.session.data should be(source.data)
      val userTicketData = parseUserTicketData(result.userTicket.get)
      userTicketData._1 should be(userId)

      val session = sessionService.get(result.session.id).futureValue
      parseUserTicketData(result.userTicket.get) shouldBe parseUserTicketData(session.userTicket.get)
    }
  }

  private def parseUserTicketData(jwtString: String) = {
    val i: Int = jwtString.lastIndexOf('.')
    val withoutSignature = jwtString.substring(0, i + 1)
    val parsedClaims = Jwts.parser.parseClaimsJwt(withoutSignature)
    val jwtsBody = parsedClaims.getBody
    val sessionResultRaw = jwtsBody.get("sessionResult").asInstanceOf[String]
    val sessionResultBytes = Base64.getDecoder.decode(sessionResultRaw)

    val sessionResult = SessionResult.parseFrom(sessionResultBytes)
    (
      jwtsBody.get("uid").asInstanceOf[String],
      jwtsBody.get("type").asInstanceOf[String],
      jwtsBody.get("version").asInstanceOf[String],
      sessionResult
    )
  }

}
