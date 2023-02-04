package ru.auto.tests.report

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import io.qameta.allure.junit4.DisplayName
import org.assertj.Assertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, validatedWith}
import ru.auto.tests.constants.Owners._
import ru.auto.tests.model.AutoApiVinOwnerItem.RegistrationStatusEnum
import ru.auto.tests.model.AutoApiVinPtsOwnersBlock.OwnersCountStatusEnum
import ru.auto.tests.model.{
  AutoApiOffer,
  AutoApiVinConstraintBlock,
  AutoApiVinEssentialsOfferReportRequest,
  AutoApiVinOfferReportRequest,
  AutoApiVinOwnerType,
  AutoApiVinRawReportResponse
}
import ru.auto.tests.model.AutoApiVinRawVinReport.ReportTypeEnum.{FREE_REPORT, PAID_REPORT}
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.report.ReportUtils.IgnoreFields
import ru.auto.tests.utils.FileUtil

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters.CollectionHasAsScala

@DisplayName("POST /report/raw/for-offer/fake")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetFakeReportForOfferTest {

  private val VIN = "Z8T4DNFUCDM014995"
  private val MASKED_VIN = "Z8T**************"
  private val OFFER_FILE_PATH = "offers/Z8T4DNFUCDM014995.json"
  private def getOffer = FileUtil.loadOfferFromFile(OFFER_FILE_PATH)

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
  def shouldSeeFakeOfferReportAndMaskedVin(): Unit = {
    val response = getFakeOfferReport(api, getOffer)
    assertThat(response.getReport).hasReportType(FREE_REPORT)
    assertThat(response.getReport.getPtsInfo).hasVin(MASKED_VIN)
  }

  @Test
  @Owner(CARFAX)
  def shouldFakeOwners(): Unit = {
    val o = getOffer
    val offerOwnersCount = o.getDocuments.getOwnersNumber
    val ptsOwnersBlock = getFakeOfferReport(api, o).getReport.getPtsOwners
    assert(ptsOwnersBlock.getOwnersCountStatus == OwnersCountStatusEnum.OK)
    assert(ptsOwnersBlock.getOwnersCountOffer == offerOwnersCount)
    assert(ptsOwnersBlock.getOwnersCountReport == offerOwnersCount)
    assert(ptsOwnersBlock.getOwners.size == offerOwnersCount)
    assert(
      ptsOwnersBlock.getOwners.asScala.forall { owner =>
        owner.getOwnerType.getType == AutoApiVinOwnerType.TypeEnum.PERSON &&
        owner.getRegistrationStatus == RegistrationStatusEnum.REGISTERED
      }
    )
  }

  @Test
  @Owner(CARFAX)
  def shouldFakeConstraint(): Unit = {
    val fakeableVinOffer = getOffer
    fakeableVinOffer.getDocuments.setVin("Z8T4DNFUCDM014830") //хэш вина % 100 < 5
    val fakeResponse = getFakeOfferReport(api, fakeableVinOffer)
    val trueResponse = getFakeOfferReport(api, getOffer)
    assert(fakeResponse.getReport.getConstraints.getStatus == AutoApiVinConstraintBlock.StatusEnum.ERROR)
    assert(trueResponse.getReport.getConstraints.getStatus == AutoApiVinConstraintBlock.StatusEnum.OK)
  }

  @Test
  @Owner(CARFAX)
  def shouldFakePts(): Unit = {
    val o = getOffer
    val ptsBlock = getFakeOfferReport(api, o).getReport.getPtsInfo
    assert(ptsBlock.getMark.getValue == o.getCarInfo.getMark)
    assert(ptsBlock.getModel.getValue == o.getCarInfo.getModel)
    assert(ptsBlock.getYear.getValue == o.getDocuments.getYear)
    assert(ptsBlock.getDisplacement.getValue == o.getCarInfo.getTechParam.getDisplacement)
    assert(ptsBlock.getHorsePower.getValue == o.getCarInfo.getHorsePower)
  }

  @Test
  @Owner(CARFAX)
  def shouldNotSeeDiffWithProductionFakeOfferReport(): Unit = {

    val request = (apiClient: ApiClient) =>
      apiClient.report.fakeRawReportForOffer
        .reqSpec(defaultSpec)
        .body(new AutoApiVinOfferReportRequest().offer(getOffer).vinOrLp(VIN).isPaid(false))
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IgnoreFields: _*)
    )
  }

  private def getFakeOfferReport(api: ApiClient, offer: AutoApiOffer): AutoApiVinRawReportResponse = {
    api.report.fakeRawReportForOffer
      .reqSpec(defaultSpec)
      .body(new AutoApiVinOfferReportRequest().offer(offer).vinOrLp(offer.getDocuments.getVin).isPaid(false))
      .executeAs(validatedWith(shouldBe200OkJSON))
  }
}
