package ru.auto.tests.report

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import io.qameta.allure.junit4.DisplayName
import io.restassured.builder.ResponseSpecBuilder
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND, SC_OK}
import org.assertj.Assertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.anno.Prod
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Owners.CARFAX
import ru.auto.tests.model.AutoApiVinRawReportResponse
import ru.auto.tests.model.AutoApiVinRawVinReport.ReportTypeEnum.FREE_REPORT
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.model.AutoApiVinVinDecoderError.ErrorCodeEnum.VIN_NOT_VALID
import ru.auto.tests.report.ReportUtils.IgnoreFields

import scala.annotation.meta.getter

@DisplayName("GET /report/raw/fake")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetFakeRawReportTest {

  private val vin = "Z94CC41BBER184593"
  private val validVin = "W0V1C41BBER196742"
  private val maskedVin = "W0V**************"
  private val lp = "B777BB777"
  private val maskedVinByLP = "W0L**************"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(CARFAX)
  def shouldSeeFakeReportAndMaskedVin(): Unit = {
    val response = requestForValidResponse(api, validVin, shouldBe200OkJSON)
    assertThat(response.getReport).hasReportType(FREE_REPORT)
    assertThat(response.getReport.getPtsInfo).hasVin(maskedVin)
  }

  @Test
  @Owner(CARFAX)
  def shouldSeeFakeReportWhenRequestingByLP(): Unit = {
    val response = requestForValidResponse(api, lp, shouldBe200OkJSON)
    assertThat(response.getReport).hasReportType(FREE_REPORT)
    assertThat(response.getReport.getPtsInfo).hasVin(maskedVinByLP)
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400ForInvalidVinOrLP(): Unit = {
    val response = api.report
      .rawFakeReport()
      .reqSpec(defaultSpec)
      .vinOrLicensePlateQuery(getRandomString)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
    assertThat(response.getError).hasErrorCode(VIN_NOT_VALID)
  }

  @Test
  @Owner(CARFAX)
  def shouldNotSeeDiffWithProductionForUnknownVin(): Unit = {
    val request = (apiClient: ApiClient) => requestForValidJsonResponse(apiClient, vin, shouldBeCode(SC_NOT_FOUND))

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi))
    )

  }

  @Test
  @Owner(CARFAX)
  def shouldNotSeeDiffWithProductionForValidVin(): Unit = {
    val request = (apiClient: ApiClient) => requestForValidJsonResponse(apiClient, validVin, shouldBeCode(SC_OK))

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IgnoreFields: _*)
    )

  }

  /**
    * Хак для фильтрации случайных 202 ответов
    */
  private def requestForValidJsonResponse(apiClient: ApiClient, vinOrLP: String, r: ResponseSpecBuilder): JsonObject = {
    try {
      apiClient.report
        .rawFakeReport()
        .reqSpec(defaultSpec)
        .vinOrLicensePlateQuery(vinOrLP)
        .execute(validatedWith(r))
        .as(classOf[JsonObject])
    } catch {
      case exp: java.lang.AssertionError if exp.getMessage.contains("202") =>
        requestForValidJsonResponse(apiClient, vinOrLP, r)
    }
  }

  private def requestForValidResponse(
      apiClient: ApiClient,
      vinOrLP: String,
      r: ResponseSpecBuilder): AutoApiVinRawReportResponse = {
    try {
      apiClient.report
        .rawFakeReport()
        .reqSpec(defaultSpec)
        .vinOrLicensePlateQuery(vinOrLP)
        .executeAs(validatedWith(r))
    } catch {
      case exp: java.lang.AssertionError if exp.getMessage.contains("202") =>
        requestForValidResponse(apiClient, vinOrLP, r)
    }
  }

}
