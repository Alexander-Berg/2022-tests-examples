package ru.auto.tests.autostrategies

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.common.collect.Lists.newArrayList
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.adaptor.SalesmanApiAdaptor
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.model.AutostrategyIdView
import ru.auto.tests.model.AutostrategyIdView.AutostrategyTypeEnum.ALWAYS_AT_FIRST_PAGE
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe200WithMessageOK

import scala.annotation.meta.getter

@DisplayName("PUT /service/{service}/autostrategies/delete")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteAutostrategiesTest {

  private val OFFER_ID = "1095858148-3a897e9c"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val salesmanAdaptor: SalesmanApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldDeleteAutostrategiesRouteHasNoDiffWithProduction(): Unit = {
    salesmanAdaptor.createAutostrategies(OFFER_ID)

    api.autostrategies
      .deleteAutostrategiesRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .body(
        newArrayList(
          new AutostrategyIdView()
            .offerId(OFFER_ID)
            .autostrategyType(ALWAYS_AT_FIRST_PAGE)
        )
      )
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200WithMessageOK))

    val response = salesmanAdaptor.getAutostrategies(OFFER_ID)

    assertThat(response.size).isEqualTo(0)
  }
}
