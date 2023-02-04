package ru.auto.tests.report

import com.carlosbecker.guice.GuiceModules
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.constants.Owners.SIEVMI
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.report.ReportUtils.IgnoreFields

object GibddReportRawDiffTest {

  @Parameterized.Parameters(name = "{index}: {0}")
  def getParameters: Array[String] = Array("X4XKC81180C572568", "Z94CT41DADR245994", "X9FMXXEEBMDM05436")
}

@DisplayName("GET /report/raw/gibdd?vin={vin}")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class GibddReportRawDiffTest(vin: String) {

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  private val ignoredFields = Array(
    "report.header.timestampUpdate",
    "report.sources.header.timestampUpdate",
    "report.pledge.header.timestampUpdate"
  )

  @Test
  @Owner(SIEVMI)
  def shouldNotSeeDiffWithProductionReport(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient.report
        .rawGibddReport()
        .reqSpec(defaultSpec)
        .vinQuery(vin)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(ignoredFields: _*)
    )
  }

}
