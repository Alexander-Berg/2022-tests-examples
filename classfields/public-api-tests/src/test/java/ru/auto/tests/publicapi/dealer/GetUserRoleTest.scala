package ru.auto.tests.publicapi.dealer

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.model.AutoApiCabinetUserRoleUserRoleResponse.RoleEnum
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.CLIENT_NOT_FOUND
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts
import ru.auto.tests.utils.AssertUtils.assertApiError

import scala.annotation.meta.getter

@DisplayName("GET /cabinet/user/role")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetUserRoleTest {

  private val managerAccount = DealerAccounts.getManagerAccount
  private val moderatorAccount = DealerAccounts.getModeratorAccount
  private val dealerAccount = DealerAccounts.getDemoAccount
  private val agencyAccount = DealerAccounts.getAgencyAccount

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

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessManagerRole(): Unit = {
    val managerSessionId = adaptor.login(managerAccount).getSession.getId

    val response =
      api
        .cabinet()
        .userRole()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(managerSessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response).hasRole(RoleEnum.MANAGER)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessClientRole(): Unit = {
    val dealerSessionId = adaptor.login(dealerAccount).getSession.getId

    val response =
      api
        .cabinet()
        .userRole()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(dealerSessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response).hasRole(RoleEnum.CLIENT)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessAgencyRole(): Unit = {
    val agencySessionId = adaptor.login(agencyAccount).getSession.getId

    val response =
      api
        .cabinet()
        .userRole()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(agencySessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response).hasRole(RoleEnum.AGENCY)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessModeratorRole(): Unit = {
    val moderatorSessionId = adaptor.login(moderatorAccount).getSession.getId

    val response =
      api
        .cabinet()
        .userRole()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(moderatorSessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response).hasRole(RoleEnum.MANAGER)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee404ForPrivate(): Unit = {
    val privateSessionId = adaptor.login(accountManager.create()).getSession.getId

    assertApiError(CLIENT_NOT_FOUND) {
      api
        .cabinet()
        .userRole()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(privateSessionId)
        .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldHasNoDiffWithProduction(): Unit = {
    val managerSessionId = adaptor.login(managerAccount).getSession.getId

    val req = (apiClient: ApiClient) =>
      apiClient
        .cabinet()
        .userRole()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(managerSessionId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
