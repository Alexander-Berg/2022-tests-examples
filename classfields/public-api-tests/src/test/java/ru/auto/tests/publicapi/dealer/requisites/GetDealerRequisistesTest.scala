package ru.auto.tests.publicapi.dealer.requisites

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.{Description, Owner}
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND, SC_OK, SC_UNAUTHORIZED}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.testdata.DealerAccounts
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.utils.AssertUtils.assertApiError
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{CLIENT_NOT_FOUND, CUSTOMER_ACCESS_FORBIDDEN}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.model.BillingRequisitesRequisitesResponse
import scala.annotation.meta.getter

@DisplayName("GET /dealer/requisites")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetDealerRequisistesTest {

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

  @Inject
  private val accountManager: AccountManager = null

  private val dealerAccount = DealerAccounts.getDemoAccount

  @Test
  @Owner(DEALER_PRODUCTS)
  @Description("Check status code 200 and response for GET /dealer/requisites (OK)")
  def shouldSee200DealerRequisites(): Unit = {
    val dealerSessionId = adaptor.login(dealerAccount).getSession.getId
    val res = api.dealerRequisites.dealerRequisites
      .reqSpec(defaultSpec)
      .xSessionIdHeader(dealerSessionId)
      .executeAs(validatedWith(shouldBeCode(SC_OK)))

    val req = res.getRequisitesList

    MatcherAssert.assertThat("Should have some payment requisites", req.size() > 0)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  @Description("Check status code 403 and response for GET /dealer/requisites (FORBIDDEN)")
  def shouldSee403DealerRequisites(): Unit = {
    val dealerSessionId = adaptor.login(accountManager.create()).getSession.getId
    assertApiError(CUSTOMER_ACCESS_FORBIDDEN) {
      api.dealerRequisites.dealerRequisites
        .reqSpec(defaultSpec)
        .xSessionIdHeader(dealerSessionId)
        .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
    }

  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldHasNoDiffWithProduction(): Unit = {
    val dealerSessionId = adaptor.login(dealerAccount).getSession.getId

    val req = (apiClient: ApiClient) =>
      apiClient.dealerRequisites.dealerRequisites
        .reqSpec(defaultSpec)
        .xSessionIdHeader(dealerSessionId)
        .execute(validatedWith(shouldBeCode(SC_OK)))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }

}
