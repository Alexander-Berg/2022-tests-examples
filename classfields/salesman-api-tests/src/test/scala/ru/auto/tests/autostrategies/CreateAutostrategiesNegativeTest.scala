package ru.auto.tests.autostrategies

import java.time.LocalDate

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.common.collect.Lists.newArrayList
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.model.{AlwaysAtFirstPagePayloadView, AutostrategyView}
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe400WithMissedSalesmanUser

import scala.annotation.meta.getter

@DisplayName("PUT /service/{service}/autostrategies")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[SalesmanApiModule]))
class CreateAutostrategiesNegativeTest {

  private val AUTOSTRATEGY = new AutostrategyView()
    .offerId("1095858148-3a897e9c")
    .fromDate(LocalDate.now)
    .toDate(LocalDate.now.plusDays(1L))
    .maxApplicationsPerDay(1)
    .alwaysAtFirstPage(
      new AlwaysAtFirstPagePayloadView()
        .forMarkModelListing(true)
        .forMarkModelGenerationListing(true)
    )

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithMissedSalesmanUserHeader(): Unit =
    api.autostrategies.putAutostrategiesRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .body(newArrayList(AUTOSTRATEGY))
      .execute(validatedWith(shouldBe400WithMissedSalesmanUser))

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithMissedBody(): Unit =
    api.autostrategies.putAutostrategiesRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
}
