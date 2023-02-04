package ru.auto.salesman.client.uaas.impl

import cats.data.NonEmptySet
import com.google.common.io.BaseEncoding
import ru.auto.salesman.client.uaas.UaasClient.UaasClientException.ParsingException
import ru.auto.salesman.model.user.Experiment
import ru.auto.salesman.model.user.product.{AutoruProduct, ProductProvider}
import ru.auto.salesman.model.RegionId
import ru.auto.salesman.test.BaseSpec
import sttp.model.Header

class UaasExperimentExtractorSpec extends BaseSpec {
  import UaasExperimentExtractorSpec._

  "parse info from header" in {
    val experiments =
      UaasExperimentExtractor
        .extractExperiments(testExperimentBase64)
        .success
        .value

    experiments should contain theSameElementsAs List(expectedFullExperiment)
  }

  "parse info without option fields" in {
    val experiments =
      UaasExperimentExtractor
        .extractExperiments(testExperimentWithoutOptionFieldsBase64)
        .success
        .value

    experiments should contain theSameElementsAs List(expectedShortExperiment)
  }

  "parse two experiments in one header" in {
    val experiments =
      UaasExperimentExtractor
        .extractExperiments(
          s"$testExperimentWithoutOptionFieldsBase64,$testExperimentBase64"
        )
        .success
        .value

    experiments should contain theSameElementsAs List(
      expectedShortExperiment,
      expectedFullExperiment
    )
  }

  "fail with ParsingException on bad base64" in {
    val result =
      UaasExperimentExtractor
        .extractExperiments("bla-bla")
        .failure
        .exception
    result shouldBe an[ParsingException]
  }

  "fail with ParsingException on bad json" in {
    val result =
      UaasExperimentExtractor
        .extractExperiments(badJsonBase64)
        .failure
        .exception
    result shouldBe an[ParsingException]
  }

  "fail with ParsingException on incorrect object structure" in {
    val result =
      UaasExperimentExtractor
        .extractExperiments(incorrectJsonObjectBase64)
        .failure
        .exception
    result shouldBe an[ParsingException]
  }

  "don`t parse experiment for other handlers" in {
    val experiments =
      UaasExperimentExtractor
        .extractExperiments(notOurExperimentBase64)
        .success
        .value

    experiments shouldBe empty
  }

  "return empty on empty headers" in {
    val experiments =
      UaasExperimentExtractor
        .extractExperiments("")
        .success
        .value

    experiments shouldBe empty
  }
}

object UaasExperimentExtractorSpec {

  val boxesHeaderValue = "413159,0,68;"

  val boxesHeader: Header =
    Header("header name isn't checked", boxesHeaderValue)

  val testExperimentBase64: String = BaseEncoding
    .base64()
    .encode("""[
              |  {
              |    "HANDLER": "AUTORU_SALESMAN_USER",
              |    "CONTEXT": {
              |      "MAIN": {
              |        "AUTORU_SALESMAN_USER": {
              |          "experimentId": "full_experiment",
              |          "products":"boost, badge",
              |          "geoIds": "213,1,3,225,10001,10000"
              |        }
              |      }
              |    }
              |  }
              |]
              |""".stripMargin.getBytes())

  val expectedFullExperiment: Experiment = Experiment(
    id = "full_experiment",
    geoIds = Some(
      NonEmptySet.of(
        RegionId(213),
        RegionId(1),
        RegionId(3),
        RegionId(225),
        RegionId(10001),
        RegionId(10000)
      )(cats.Order.fromOrdering)
    ),
    experimentProducts = Some(
      NonEmptySet.of[AutoruProduct](
        ProductProvider.AutoruGoods.Boost,
        ProductProvider.AutoruGoods.Badge
      )
    )
  )

  val testExperimentWithoutOptionFieldsBase64: String = BaseEncoding
    .base64()
    .encode("""[
              |  {
              |    "HANDLER": "AUTORU_SALESMAN_USER",
              |    "CONTEXT": {
              |      "MAIN": {
              |        "AUTORU_SALESMAN_USER": {
              |          "experimentId": "short_experiment",
              |          "newField": "testValue"
              |        }
              |      }
              |    }
              |  }
              |]
              |""".stripMargin.getBytes())

  val expectedShortExperiment: Experiment = Experiment(
    id = "short_experiment",
    geoIds = None,
    experimentProducts = None
  )

  val badJsonBase64: String = BaseEncoding
    .base64()
    .encode("incorrect json".getBytes())

  val incorrectJsonObjectBase64: String = BaseEncoding
    .base64()
    .encode("""[
              |  {
              |    "HANDLER": "AUTORU_SALESMAN_USER",
              |    "BAD_CONTEXT": {
              |      "AUTORU_SALESMAN_USER": {
              |        "experimentId": "VSMONEY-1234_test_price_experiment"
              |      }
              |    }
              |  }
              |]
              |""".stripMargin.getBytes())

  val notOurExperimentBase64: String = BaseEncoding
    .base64()
    .encode("""[
              |  {
              |    "HANDLER": "AUTO_RU",
              |    "CONTEXT": {
              |      "MAIN": {
              |        "AUTO_RU": {
              |          "experimentId": "not_experiment"
              |        }
              |      }
              |    }
              |  }
              |]
              |""".stripMargin.getBytes())

}
