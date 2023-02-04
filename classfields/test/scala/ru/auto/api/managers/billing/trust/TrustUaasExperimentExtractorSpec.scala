package ru.auto.api.managers.billing.trust

import akka.http.scaladsl.model.headers.RawHeader
import org.apache.commons.codec.binary.Base64
import ru.auto.api.BaseSpec
import ru.auto.api.billing.trust.TrustUaasExperimentExtractor
import ru.auto.api.model.uaas.UaasResponseHeaders

class TrustUaasExperimentExtractorSpec extends BaseSpec {

  import TrustUaasExperimentExtractorSpec._

  "TrustUaasExperimentExtractor.existsTrustExperiment" should {
    "return true" when {
      "get only trust bucket" in {
        val result = TrustUaasExperimentExtractor
          .existsTrustExperiment(getExpFlagsHeader(trustExpFlags))
        result shouldBe true
      }
      "get two different experiments" in {
        val result = TrustUaasExperimentExtractor
          .existsTrustExperiment(getExpFlagsHeader(twoExpFlags))
        result shouldBe true
      }
    }
    "return false" when {
      "empty headers" in {
        val result = TrustUaasExperimentExtractor
          .existsTrustExperiment(Seq.empty[RawHeader])
        result shouldBe false
      }
      "empty expFlags header" in {
        val result = TrustUaasExperimentExtractor
          .existsTrustExperiment(Seq(RawHeader("test", "test")))
        result shouldBe false
      }
      "fail parsing json" in {
        val result = TrustUaasExperimentExtractor
          .existsTrustExperiment(getExpFlagsHeader(badJsonExpFlags))
        result shouldBe false
      }
      "don't get trust experiment" in {
        val result = TrustUaasExperimentExtractor
          .existsTrustExperiment(getExpFlagsHeader(priceExpFlags))
        result shouldBe false
      }
      "get not trust basket" in {
        val result = TrustUaasExperimentExtractor
          .existsTrustExperiment(getExpFlagsHeader(falseTrustExpFlags))
        result shouldBe false
      }
    }
  }

  private def getExpFlagsHeader(value: String) = {
    Seq(RawHeader(UaasResponseHeaders.expFlags, value))
  }
}

object TrustUaasExperimentExtractorSpec {

  val falseTrustExpFlags = new String(
    Base64.encodeBase64("""
                          |[
                          |  {
                          |    "HANDLER": "AUTORU_SALESMAN_USER",
                          |    "CONTEXT": {
                          |      "MAIN": {
                          |        "AUTORU_TRUST": {
                          |          "isTrust": "false"
                          |        }
                          |      }
                          |    }
                          |  }
                          |]
                          |""".stripMargin.getBytes("UTF-8")),
    "UTF-8"
  )

  val trustExpFlags = new String(
    Base64.encodeBase64("""
                          |[
                          |  {
                          |    "HANDLER": "AUTORU_SALESMAN_USER",
                          |    "CONTEXT": {
                          |      "MAIN": {
                          |        "AUTORU_TRUST": {
                          |          "isTrust": "true"
                          |        }
                          |      }
                          |    }
                          |  }
                          |]
                          |""".stripMargin.getBytes("UTF-8")),
    "UTF-8"
  )

  val priceExpFlags = new String(
    Base64.encodeBase64("""
                          |[
                          |  {
                          |    "HANDLER": "AUTORU_SALESMAN_USER",
                          |    "CONTEXT": {
                          |      "MAIN": {
                          |        "AUTORU_SALESMAN_USER": {
                          |          "experimentId": "full_experiment",
                          |          "products": "boost, badge",
                          |          "geoIds": "213,1,3,225,10001,10000"
                          |        }
                          |      }
                          |    }
                          |  }
                          |]
                          |""".stripMargin.getBytes("UTF-8")),
    "UTF-8"
  )

  val twoExpFlags: String = Seq(priceExpFlags, trustExpFlags).mkString(",")

  val badJsonExpFlags = new String(
    Base64.encodeBase64("incorrect json".getBytes("UTF-8")),
    "UTF-8"
  )
}
