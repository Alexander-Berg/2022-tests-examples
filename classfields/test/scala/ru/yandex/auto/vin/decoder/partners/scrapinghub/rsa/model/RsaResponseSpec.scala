package ru.yandex.auto.vin.decoder.partners.scrapinghub.rsa.model

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.enablers.Emptiness.emptinessOfGenTraversable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json

class RsaResponseSpec extends AnyWordSpecLike with Matchers {

  "RsaResponse" should {

    "be correctly parsed from SH RSA current insurances response" in {

      val raw = ResourceUtils.getStringFromResources("/sh_rsa/current_insurances.json")

      val res = Json.parse(raw).as[RsaResponse]
      (res.requestResult should have).length(1)
      (res.requestResult.head.rows should have).length(4)
    }

    "be correctly parsed from SH RSA current insurances response when current insurances is empty" in {

      val raw = ResourceUtils.getStringFromResources("/sh_rsa/empty_current_insurances.json")

      val res = Json.parse(raw).as[RsaResponse]
      (res.requestResult should have).length(1)
      res.requestResult.head.rows shouldBe empty
    }

    "be correctly parsed from SH RSA insurance details response" in {

      val raw = ResourceUtils.getStringFromResources("/sh_rsa/insurance_details.json")

      val res = Json.parse(raw).as[RsaResponse]
      (res.requestResult should have).length(2)
      (res.requestResult.head.rows should have).length(2)
      (res.requestResult(1).rows should have).length(2)
    }
  }
}
