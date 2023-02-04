package ru.yandex.vertis.passport.service.impl

import org.joda.time.Instant
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.passport.model.{BasicSessionData, JustBasicSessionData, LegacySessionId, RichSessionId, SessionUser}
import ru.yandex.vertis.passport.service.SessionIdService.{MaybeValid, NotValid, Valid}
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.util.crypt.HmacSigner

/**
  * Tests for [[RichSessionIdService]]
  *
  * @author zvez
  */
class RichSessionIdServiceSpec extends WordSpec with Matchers {

  val service = new RichSessionIdService(new HmacSigner("secret"))

  "RichSessionIdService" should {
    "generate session id" in {
      val data = ModelGenerators.basicSessionData.next
      val sessionId = service.generate(data)
      sessionId match {
        case sid: RichSessionId => checkSameData(sid, data)
        case other => fail("Unexpected: " + other)
      }
    }

    "validate session id" in {
      val data = ModelGenerators.basicSessionData.next
      val sessionId = service.generate(data)

      val validateResult = service.validate(sessionId)
      validateResult shouldBe a[Valid]
      val Valid(sidData) = validateResult
      checkSameData(sidData, data)

      val alteredSid = sessionId.copy(owner = SessionUser("someone-else"))
      service.validate(alteredSid) shouldBe NotValid
    }

    "validate legacy session id" in {
      val legacySid = LegacySessionId(ModelGenerators.userId.next, "1234123")
      service.validate(legacySid) shouldBe MaybeValid
    }

    "prolong" in {
      val session = ModelGenerators.session.next
      val prolongData = JustBasicSessionData(session.owner, Instant.now(), session.ttl)
      val prolongedSid = service.prolong(prolongData, session.id, None)
      checkSameData(prolongedSid, prolongData)
      service.validate(prolongedSid) shouldBe a[Valid]
      val Valid(newSidData) = service.validate(prolongedSid)
      checkSameData(newSidData, prolongData)
    }

  }

  private def checkSameData(d1: BasicSessionData, d2: BasicSessionData): Boolean = {
    d1.owner shouldBe d2.owner
    d1.creationDate shouldBe d2.creationDate
    d1.ttl shouldBe d2.ttl
    true
  }
}
