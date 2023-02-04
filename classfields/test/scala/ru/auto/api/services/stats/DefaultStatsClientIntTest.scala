package ru.auto.api.services.stats

import ru.auto.api.StatsModel
import ru.auto.api.exceptions.StatsBadRequestException
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.StatsSummaryParams
import ru.auto.api.services.HttpClientSuite

class DefaultStatsClientIntTest extends HttpClientSuite {

  override protected def config: HttpClientConfig = HttpClientConfig(
    "autoru-price-estimator-01-sas.test.vertis.yandex.net",
    34398
  )

  val client = new DefaultStatsClient(http)

  test("get model stats") {
    val params = StatsSummaryParams(
      Map(
        "mark" -> List("FORD"),
        "rid" -> List("213"),
        "model" -> List("FOCUS")
      )
    )
    client.getSummary(params).futureValue
  }

  test("get mark stats") {
    val params = StatsSummaryParams(
      Map(
        "mark" -> List("FORD"),
        "rid" -> List("213")
      )
    )
    client.getSummary(params).futureValue
  }

  test("get stats bad request") {
    val params = StatsSummaryParams(
      Map(
        "rid" -> List("213"),
        "model" -> List("FOCUS")
      )
    )
    client.getSummary(params).failed.futureValue shouldBe an[StatsBadRequestException]
  }

  private val predictRequest = StatsModel.PredictRequest
    .newBuilder()
    .setRid(213)
    .setTechParamId(9383468)
    .setColor("FAFBFB")
    .setOwningTime(4)
    .setOwnersCount(1)
    .setYear(2012)
    .setKmAge(50000)
    .build()
}
