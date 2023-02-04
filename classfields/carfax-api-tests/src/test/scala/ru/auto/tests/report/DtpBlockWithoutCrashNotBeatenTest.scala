package ru.auto.tests.report

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import io.qameta.allure.{Description, Owner}
import org.assertj.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.model.AutoApiVinContentBlockContentItem.StatusEnum.OK
import ru.auto.tests.model.AutoApiVinContentBlockContentItem.TypeEnum.DTP
import ru.auto.tests.model.AutoApiVinOfferReportRequest
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.utils.FileUtil

import scala.annotation.meta.getter

@DisplayName("POST /report/raw/for-offer")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DtpBlockWithoutCrashNotBeatenTest {

  private val VIN = "WDD2211941A531110"
  private val OFFER_FILE_PATH = "offers/WDD2211941A531110.json"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  @Description("Блок ДТП не обнаружены для превью")
  def shouldSeeDtpBlockNotFoundInfoNotPayed(): Unit = {
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val response = api.report
      .postRawReportForOffer()
      .reqSpec(defaultSpec)
      .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isUserOwner(false).isPaid(false))
      .executeAs(validatedWith(shouldBe200OkJSON))

    val dtpBlock = response.getReport.getContent.getItems.stream
      .filter(item => item.getType == DTP)
      .findFirst()
      .get()

    assertThat(dtpBlock)
      .hasValue("Данные о ДТП не обнаружены")
      .hasStatus(OK)
      .hasRecordCount(null)
      .hasAvailableForFree(true)
  }

  @Test
  @Owner(TIMONDL)
  @Description("Блок ДТП не обнаружены для купленного отчета")
  def shouldSeeDtpBlockNotFoundInfoPayed(): Unit = {
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val response = api.report
      .postRawReportForOffer()
      .reqSpec(defaultSpec)
      .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isPaid(true))
      .executeAs(validatedWith(shouldBe200OkJSON))

    val dtpBlock = response.getReport.getContent.getItems.stream
      .filter(item => item.getType == DTP)
      .findFirst()
      .get()

    assertThat(dtpBlock)
      .hasValue("Данные о ДТП не обнаружены")
      .hasStatus(OK)
      .hasRecordCount(null)
      .hasAvailableForFree(true)
  }

  @Test
  @Owner(TIMONDL)
  @Description("Блок ДТП не обнаружены для превью у владельца объявления")
  def shouldSeeDtpBlockNotFoundInfoNotPayedForOwner(): Unit = {
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val response = api.report
      .postRawReportForOffer()
      .reqSpec(defaultSpec)
      .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isUserOwner(true).isPaid(false))
      .executeAs(validatedWith(shouldBe200OkJSON))

    val dtpBlock = response.getReport.getContent.getItems.stream
      .filter(item => item.getType == DTP)
      .findFirst()
      .get()

    assertThat(dtpBlock)
      .hasValue("Данные о ДТП не обнаружены")
      .hasStatus(OK)
      .hasRecordCount(null)
      .hasAvailableForFree(true)
  }

  @Test
  @Owner(TIMONDL)
  @Description("Блок ДТП не обнаружены для купленного отчета у владельца объявления")
  def shouldSeeDtpBlockNotFoundInfoPayedForOwner(): Unit = {
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val response = api.report
      .postRawReportForOffer()
      .reqSpec(defaultSpec)
      .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isUserOwner(true).isPaid(true))
      .executeAs(validatedWith(shouldBe200OkJSON))

    val dtpBlock = response.getReport.getContent.getItems.stream
      .filter(item => item.getType == DTP)
      .findFirst()
      .get()

    assertThat(dtpBlock)
      .hasValue("Данные о ДТП не обнаружены")
      .hasStatus(OK)
      .hasRecordCount(null)
      .hasAvailableForFree(true)
  }

}
