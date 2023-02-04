package ru.auto.tests.report

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.assertj.Assertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Owners.TIMONDL
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import ru.auto.tests.model.AutoApiVinRawVinReport.ReportTypeEnum.{FREE_REPORT, PAID_REPORT}
import ru.auto.tests.model.AutoApiVinVinDecoderError.ErrorCodeEnum.VIN_NOT_VALID
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.report.ReportUtils.IgnoreFields

import scala.annotation.meta.getter

@DisplayName("GET /report/raw")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetReportRawTest {

  private val VIN = "Z94CC41BBER184593"
  private val MASKED_VIN = "Z94**************"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSeeFreeReportAndMaskedVin(): Unit = {
    val response = api.report.rawReport
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .isPaidQuery(false)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getReport).hasReportType(FREE_REPORT)
    assertThat(response.getReport.getPtsInfo).hasVin(MASKED_VIN)
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeePaidReport(): Unit = {
    val response = api.report.rawReport
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .isPaidQuery(true)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getReport).hasReportType(PAID_REPORT)
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidVin(): Unit = {
    val response = api.report.rawReport
      .reqSpec(defaultSpec)
      .vinQuery(getRandomString)
      .isPaidQuery(false)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

    assertThat(response.getError).hasErrorCode(VIN_NOT_VALID)
  }

  @Test
  @Owner(TIMONDL)
  def shouldNotSeeDiffWithProductionFreeReport(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient.report.rawReport
        .reqSpec(defaultSpec)
        .vinQuery(VIN)
        .isPaidQuery(false)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IgnoreFields: _*)
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldNotSeeDiffWithProductionPaidReport(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient.report.rawReport
        .reqSpec(defaultSpec)
        .vinQuery(VIN)
        .isPaidQuery(true)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IgnoreFields: _*)
    )
  }

}
