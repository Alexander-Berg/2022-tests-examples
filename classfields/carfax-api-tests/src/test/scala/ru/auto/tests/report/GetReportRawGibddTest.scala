package ru.auto.tests.report

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_OK}
import org.assertj.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Owners.{CARFAX, SIEVMI, TIMONDL}
import ru.auto.tests.model.AutoApiVinVinDecoderError.ErrorCodeEnum.VIN_NOT_VALID
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.util.Random

@DisplayName("GET /report/raw/gibdd")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetReportRawGibddTest {

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(SIEVMI)
  def shouldSeeUpdatingReport(): Unit = {

    val randomVinSuffix = s"${Random.nextInt(9000) + 1000}" // 1000-9999
    val VIN = s"WF04XXWPD46L1$randomVinSuffix"

    api
      .admin()
      .markForUpdate()
      .reqSpec(defaultSpec)
      .contextQuery("REPORT_GIBDD")
      .vinQuery(VIN)
      .execute(validatedWith(shouldBeCode(SC_OK)))

    val response = api.report
      .rawGibddReport()
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getReport.getSources).hasReadyCount(1)
    assertThat(response.getReport.getSources).hasSourcesCount(5)
    assertThat(response.getReport.getHeader).hasIsUpdating(true)

    assertThat(response.getReport.getPtsInfo.getHeader).hasIsUpdating(true)
    assertThat(response.getReport.getPtsOwners.getHeader).hasIsUpdating(true)
    assertThat(response.getReport.getConstraints.getHeader).hasIsUpdating(true)
    assertThat(response.getReport.getWanted.getHeader).hasIsUpdating(true)
    assertThat(response.getReport.getDtp.getHeader).hasIsUpdating(true)

    // блок залогов всегда готов
    assert(response.getReport.getPledge.getHeader.getIsUpdating == null)
    assert((System.currentTimeMillis() - response.getReport.getPledge.getHeader.getTimestampUpdate) <= 10000)
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400WithInvalidVin(): Unit = {
    val response = api.report.rawGibddReport
      .reqSpec(defaultSpec)
      .vinQuery(getRandomString)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

    assertThat(response.getError).hasErrorCode(VIN_NOT_VALID)
  }

}
