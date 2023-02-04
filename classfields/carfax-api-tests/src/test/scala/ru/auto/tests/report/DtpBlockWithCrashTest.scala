package ru.auto.tests.report

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.{Description, Owner}
import io.qameta.allure.junit4.DisplayName
import org.assertj.Assertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.model.AutoApiVinContentBlockContentItem.StatusEnum.ERROR
import ru.auto.tests.model.AutoApiVinContentBlockContentItem.TypeEnum.DTP
import ru.auto.tests.model.AutoApiVinOfferReportRequest
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.utils.FileUtil
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /report/raw/for-offer")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DtpBlockWithCrashTest {

  private val VIN = "Z94K241BAKR092916"
  private val OFFER_FILE_PATH = "offers/Z94K241BAKR092916.json"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  @Description("Отсутствие блока с ДТП инфой для превью")
  def shouldSeeDtpBlockNullNotPayed(): Unit = {
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
      .hasRecordCount(-1)
      .hasStatus(null)
      .hasAvailableForFree(null)
  }

  @Test
  @Owner(TIMONDL)
  @Description("Блок с количеством ДТП для купленного отчета")
  def shouldSeeDtpBlockInfoPayed(): Unit = {
    val offer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

    val response = api.report
      .postRawReportForOffer()
      .reqSpec(defaultSpec)
      .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(VIN).isUserOwner(false).isPaid(true))
      .executeAs(validatedWith(shouldBe200OkJSON))

    val dtpBlock = response.getReport.getContent.getItems.stream
      .filter(item => item.getType == DTP)
      .findFirst()
      .get()

    assertThat(dtpBlock)
      .hasValue("Автомобиль побывал в 1 ДТП")
      .hasRecordCount(1)
      .hasStatus(ERROR)
      .hasAvailableForFree(true)
    Assertions.assertThat(response.getReport.getDtp.getItems).hasSize(1)
  }

  @Test
  @Owner(TIMONDL)
  @Description("Блок с количеством ДТП для превью у владельца объявления")
  def shouldSeeDtpBlockInfoForOwnerNotPayed(): Unit = {
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
      .hasValue("Автомобиль побывал в 1 ДТП")
      .hasRecordCount(1)
      .hasStatus(ERROR)
      .hasAvailableForFree(true)
    Assertions.assertThat(response.getReport.getDtp.getItems).hasSize(1)
  }

  @Test
  @Owner(TIMONDL)
  @Description("Блок с количеством ДТП для купленного отчета у владельца объявления")
  def shouldSeeDtpBlockInfoForOwnerPayed(): Unit = {
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
      .hasValue("Автомобиль побывал в 1 ДТП")
      .hasRecordCount(1)
      .hasStatus(ERROR)
      .hasAvailableForFree(true)
    Assertions.assertThat(response.getReport.getDtp.getItems).hasSize(1)
  }

}
