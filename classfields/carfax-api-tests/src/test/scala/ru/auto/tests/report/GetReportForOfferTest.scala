package ru.auto.tests.report

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.Assertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.constants.Owners._
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import ru.auto.tests.model.{AutoApiVinEssentialsOfferReportRequest, AutoApiVinOfferReportRequest}
import ru.auto.tests.model.AutoApiVinRawVinReport.ReportTypeEnum.{FREE_REPORT, PAID_REPORT}
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.report.ReportUtils.IgnoreFields
import ru.auto.tests.utils.FileUtil

import scala.annotation.meta.getter

@DisplayName("POST /report/raw/for-offer")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetReportForOfferTest {

  private val VIN = "Z8T4DNFUCDM014995"
  private val MASKED_VIN = "Z8T**************"
  private val OFFER_FILE_PATH = "offers/Z8T4DNFUCDM014995.json"

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
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val response = api.report
      .postRawReportForOffer()
      .reqSpec(defaultSpec)
      .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isPaid(false))
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getReport).hasReportType(FREE_REPORT)
    assertThat(response.getReport.getPtsInfo).hasVin(MASKED_VIN)
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeePaidReport(): Unit = {
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val response = api.report
      .postRawReportForOffer()
      .reqSpec(defaultSpec)
      .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isPaid(true))
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getReport).hasReportType(PAID_REPORT)
  }

  @Test
  @Owner(TIMONDL)
  def shouldNotSeeDiffWithProductionFreeReport(): Unit = {
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val request = (apiClient: ApiClient) =>
      apiClient.report
        .postRawReportForOffer()
        .reqSpec(defaultSpec)
        .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isPaid(false))
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
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val request = (apiClient: ApiClient) =>
      apiClient.report
        .postRawReportForOffer()
        .reqSpec(defaultSpec)
        .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isPaid(true))
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IgnoreFields: _*)
    )
  }

  @Test
  @Owner(CARFAX)
  def shouldNotSeeDiffWithProductionFreeReportAnonUser(): Unit = {
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val anonUserId = "anon:g60aecad31ql5oqqq3urb9def4r"

    val request = (apiClient: ApiClient) =>
      apiClient.report
        .postRawReportForOffer()
        .reqSpec(defaultSpec)
        .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isPaid(false).userId(anonUserId))
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IgnoreFields: _*)
    )
  }

  @Test
  @Owner(CARFAX)
  def shouldNotSeeDiffWithProductionEssentialsReportAnonUser(): Unit = {
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val anonUserId = "anon:g60aecad31ql5oqqq3urb9def4r"

    val request = (apiClient: ApiClient) =>
      apiClient.report
        .rawEssentialsReportForOffer()
        .reqSpec(defaultSpec)
        .xUserIdHeader(anonUserId)
        .body(new AutoApiVinEssentialsOfferReportRequest().offer(offer).vinOrLp(VIN))
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IgnoreFields: _*)
    )
  }

}
