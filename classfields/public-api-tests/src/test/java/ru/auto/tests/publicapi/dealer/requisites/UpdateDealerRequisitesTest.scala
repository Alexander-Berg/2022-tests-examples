package ru.auto.tests.publicapi.dealer.requisites

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.{Description, Owner}
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_OK, SC_UNAUTHORIZED}
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
import ru.auto.tests.utils.AssertUtils.assertApiError
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{CUSTOMER_ACCESS_FORBIDDEN, NO_AUTH}
import ru.auto.tests.publicapi.model.{BillingRequisitesIndividual, BillingRequisitesRequisitesIdResponse, BillingRequisitesRequisitesProperties}

import scala.annotation.meta.getter

@DisplayName("PUT /dealer/requisites/{requisites_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UpdateDealerRequisitesTest {

  private val requisitesId = 8806377

  private val individual = new BillingRequisitesIndividual()
    .email("demo@auto.ru")
    .firstName("тест")
    .midName("тестович")
    .lastName("тестовый")
    .phone("74440000000")

  private val requisites = new BillingRequisitesRequisitesProperties().individual(individual)

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
  @Description("Check status code 200 and response for PUT /dealer/requisites/{requisites_id} (OK)")
  def shouldSee200DealerRequisites(): Unit = {
    val dealerSessionId = adaptor.login(dealerAccount).getSession.getId
    val res = api.dealerRequisites
      .updateRequisites()
      .requisitesIdPath(requisitesId)
      .body(requisites)
      .reqSpec(defaultSpec)
      .xSessionIdHeader(dealerSessionId)
      .executeAs(validatedWith(shouldBeCode(SC_OK)))

    MatcherAssert.assertThat("should return the updated id", res.getId == requisitesId)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  @Description("Check status code 403 and response for PUT /dealer/requisites/{requisites_id} (FORBIDDEN)")
  def shouldSee403WithoutSessionId(): Unit = {
    val managerSessionId = adaptor.login(accountManager.create()).getSession.getId
    assertApiError(CUSTOMER_ACCESS_FORBIDDEN) {
      api
        .dealerRequisites()
        .updateRequisites()
        .requisitesIdPath(requisitesId)
        .body(requisites)
        .reqSpec(defaultSpec)
        .xSessionIdHeader(managerSessionId)
        .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  @Description("Check status code 401 and response for PUT /dealer/requisites/{requisites_id} (UNAUTHORIZED)")
  def shouldSee401WithoutSessionId(): Unit =
    assertApiError(NO_AUTH) {
      api
        .dealerRequisites()
        .updateRequisites()
        .requisitesIdPath(requisitesId)
        .body(requisites)
        .reqSpec(defaultSpec)
        .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
    }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeNoDifferentWithProduct(): Unit = {
    val dealerSessionId = adaptor.login(dealerAccount).getSession.getId

    val req = (apiClient: ApiClient) =>
      apiClient.dealerRequisites
        .updateRequisites()
        .requisitesIdPath(requisitesId)
        .body(requisites)
        .reqSpec(defaultSpec)
        .xSessionIdHeader(dealerSessionId)
        .execute(validatedWith(shouldBeCode(SC_OK)))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req(api), jsonEquals[JsonObject](req(prodApi)))
  }

}
