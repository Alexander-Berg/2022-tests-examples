package ru.auto.tests.report

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_OK}
import org.assertj.Assertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Owners.CARFAX
import ru.auto.tests.model.AutoApiVinVinDecoderError.ErrorCodeEnum.VIN_NOT_VALID
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.report.ReportUtils.IgnoreFields

@DisplayName("GET /report/raw/cmexpert")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetReportRawCMExpertTest {

  private val VIN = "Z8T4DNFUCDM014995"

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(CARFAX)
  def shouldSee400WithInvalidVin(): Unit = {
    val response = api.report.rawCMExpertReport
      .reqSpec(defaultSpec)
      .vinQuery(getRandomString)
      .orderIdQuery(getRandomString)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

    assertThat(response.getError).hasErrorCode(VIN_NOT_VALID)
  }

  @Test
  @Owner(CARFAX)
  def shouldNotSeeDiffWithProductionCMExpertReport(): Unit = {

    val request = (apiClient: ApiClient) =>
      apiClient.report.rawCMExpertReport
        .reqSpec(defaultSpec)
        .vinQuery(VIN)
        .orderIdQuery("123")
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IgnoreFields: _*)
    )
  }

  @Test
  @Owner(CARFAX)
  def shouldSeeUpdatingReport(): Unit = {

    val orderId = "123-abc-def"

    api
      .admin()
      .markForUpdate()
      .reqSpec(defaultSpec)
      .contextQuery("REPORT_CM_EXPERT")
      .vinQuery(VIN)
      .execute(validatedWith(shouldBeCode(SC_OK)))

    val response = api.report.rawCMExpertReport
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .orderIdQuery(orderId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getReport.getHeader).hasIsUpdating(true)
    assert(response.getReport.getPdfUrl.contains(orderId))

    assert(
      Option(response.getReport.getPledge)
        .map(_.getHeader)
        .map(_.getIsUpdating)
        .forall(_ == null)
    )
    assert(
      Option(response.getReport.getCustoms)
        .map(_.getHeader)
        .map(_.getIsUpdating)
        .forall(_ == null)
    )
    List(
      Option(response.getReport.getPledge).map(_.getHeader),
      Option(response.getReport.getCustoms).map(_.getHeader)
    ).collect {
      case Some(h) if h != null => h
    }.map(_.getTimestampUpdate)
      .foreach { updatedAt =>
        assert(System.currentTimeMillis - updatedAt <= 10000)
      }
  }
}
