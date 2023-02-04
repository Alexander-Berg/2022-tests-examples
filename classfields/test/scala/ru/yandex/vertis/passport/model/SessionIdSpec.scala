package ru.yandex.vertis.passport.model

import org.joda.time.{Duration, Instant}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, Matchers}
import ru.yandex.vertis.passport.test.ModelGenerators

/**
  * Tests for session id formats
  *
  * @author zvez
  */
//scalastyle:off multiple.string.literals
class SessionIdSpec extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  "Legacy session id:" - {
    "parse" in {
      val sid = SessionId.parse("19084139.e2fbc61dfa5dcdee_a38e5c88886f7cc4c995f5f3e499efa2")
      sid shouldBe LegacySessionId("19084139", "e2fbc61dfa5dcdee_a38e5c88886f7cc4c995f5f3e499efa2")
    }

    "recreate" in {
      LegacySessionId("user", "random").asString shouldBe "user.random"
    }
  }

  "Simple session id:" - {
    "parse" in {
      val sid = SessionId.parse("19084139|a38e5c88886f7cc4c995f5f3e499efa2")
      sid shouldBe SimpleSessionId("19084139", "a38e5c88886f7cc4c995f5f3e499efa2")
    }

    "recreate" in {
      val sid = SimpleSessionId("user", "random")
      sid.asString shouldBe "user|random"
      sid shouldBe SessionId.parse(sid.asString)
    }
  }

  "Rich session id:" - {
    "recreate" in {
      val sid = RichSessionId(SessionUser("user"), Instant.now(), Duration.standardDays(7), "123", "sign")
      sid shouldBe SessionId.parse(sid.asString)
    }
  }

  "generic" in {
    forAll(ModelGenerators.fakeSessionId) { sid =>
      sid shouldBe SessionId.parse(sid.asString)
    }
  }

}
