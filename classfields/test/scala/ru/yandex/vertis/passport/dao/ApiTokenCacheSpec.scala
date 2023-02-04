package ru.yandex.vertis.passport.dao

import org.scalacheck.Gen
import org.scalatest.FreeSpec
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.model.tokens.{ApiTokenFormats, ApiTokenRemove}
import ru.yandex.vertis.passport.test.ModelGenerators.{ApiTokenGen, ApiTokenUpdateGen}
import ru.yandex.vertis.passport.test.SpecBase

import scala.concurrent.Future

trait ApiTokenCacheSpec extends FreeSpec with SpecBase with ApiTokenFormats {

  def tokenCache: ApiTokenCache

  "ApiTokenDao" - {
    "return None if token not found" in {
      tokenCache.get("some_test_id").futureValue shouldBe None
    }

    "insert and find token" in {
      val token = ApiTokenWriter.write(ApiTokenGen.next)
      tokenCache.upsert(token).futureValue
      val res = tokenCache.get(token.getId).futureValue

      res.get.getId shouldEqual token.getId
      res.get.getPayload shouldEqual token.getPayload
    }

    "delete" in {
      val token = ApiTokenWriter.write(ApiTokenGen.next)
      tokenCache.upsert(token).futureValue
      tokenCache.delete(token.getId).futureValue

      val res = tokenCache.get(token.getId).futureValue
      res shouldBe None
    }
  }

}
