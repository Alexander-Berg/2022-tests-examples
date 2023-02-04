package ru.auto.tests.publicapi.dealer

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.account.Account
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.model.AutoApiCabinetCustomerInfoResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.FORBIDDEN_REQUEST
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount
import ru.auto.tests.utils.AssertUtils.assertApiError

import scala.annotation.meta.getter

@DisplayName("GET /cabinet/customer")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCustomerInfoTest {

  private val agencyUserAccount =
    Account.builder().login("user.agency@auto.ru").password("autoru").id("70425462").build()

  private val companyGroupUserAccount =
    Account.builder().login("user.gk@auto.ru").password("autoru").id("70425463").build()

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessAgency(): Unit = {
    val agencySessionId = adaptor.login(agencyUserAccount).getSession.getId

    val response =
      api
        .cabinet()
        .customerInfo()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(agencySessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response)
      .hasId(49507)
      .hasName("Агентство для регрессионных тестов https://st.yandex-team.ru/VSDEALERSDECOMP-299")
      .hasType(AutoApiCabinetCustomerInfoResponse.TypeEnum.AGENCY)
      .hasBalanceAgencyId(1355613961)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessCompanyGroup(): Unit = {
    val companyGroupSessionId = adaptor.login(companyGroupUserAccount).getSession.getId

    val response =
      api
        .cabinet()
        .customerInfo()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(companyGroupSessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response)
      .hasId(336)
      .hasName("ГК для регрессионных тестов VSDEALERSDECOMP-299")
      .hasType(AutoApiCabinetCustomerInfoResponse.TypeEnum.COMPANY_GROUP)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403ForNonAgentUser(): Unit = {
    val dealerSessionId = adaptor.login(getDemoAccount).getSession.getId

    assertApiError(FORBIDDEN_REQUEST) {
      api.cabinet
        .customerInfo()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(dealerSessionId)
        .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldHasNoDiffWithProduction(): Unit = {
    val agencySessionId = adaptor.login(agencyUserAccount).getSession.getId

    val req = (apiClient: ApiClient) =>
      apiClient
        .cabinet()
        .customerInfo()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(agencySessionId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
