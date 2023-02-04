package ru.auto.tests.autostrategies

import java.time.LocalDate

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.common.collect.Lists.newArrayList
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.junit.{After, Ignore, Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.adaptor.SalesmanApiAdaptor
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.model.{AlwaysAtFirstPagePayloadView, AutostrategyView}
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe200WithMessageOK

import scala.annotation.meta.getter

@DisplayName("PUT /service/{service}/autostrategies")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[SalesmanApiModule]))
class CreateAutostrategiesTest {

  private val OFFER_ID = "1095858148-3a897e9c"
  private val FROM_DATE = LocalDate.now
  private val TO_DATE = LocalDate.now.plusDays(1L)
  private val MAX_APPLICATIONS_PER_DAY = 1

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val salesmanAdaptor: SalesmanApiAdaptor = null

  @Test
  @Ignore
  @Owner(TIMONDL)
  def shouldSee200PutAutostrategiesRoute(): Unit = {
    api.autostrategies.putAutostrategiesRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .body(
        newArrayList(
          new AutostrategyView()
            .offerId(OFFER_ID)
            .fromDate(FROM_DATE)
            .toDate(TO_DATE)
            .maxApplicationsPerDay(MAX_APPLICATIONS_PER_DAY)
            .alwaysAtFirstPage(
              new AlwaysAtFirstPagePayloadView()
                .forMarkModelListing(true)
                .forMarkModelGenerationListing(true)
            )
        )
      )
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200WithMessageOK))

    val autostrategies = salesmanAdaptor.getAutostrategies(OFFER_ID)

    val offer = autostrategies.get(0).getAsJsonObject
    val autostrategy =
      offer.getAsJsonArray("autostrategies").get(0).getAsJsonObject

    assertThat(offer.get("offerId").getAsString).isEqualTo(OFFER_ID)
    assertThat(autostrategy.get("maxApplicationsPerDay").getAsInt)
      .isEqualTo(MAX_APPLICATIONS_PER_DAY)
    assertThat(autostrategy.get("fromDate").getAsString)
      .isEqualTo(FROM_DATE.toString)
    assertThat(autostrategy.get("toDate").getAsString)
      .isEqualTo(TO_DATE.toString)
  }

  @After def deleteAutostrategy(): Unit =
    salesmanAdaptor.deleteAutostrategies(OFFER_ID)
}
