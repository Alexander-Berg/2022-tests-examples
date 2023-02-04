package ru.auto.tests.publicapi.draft

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /user/draft/{category}/vin/{vin}")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class GetDraftByVinTest {

  private val VIN = "SALWA2FK7HA135034"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val am: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.draft.draftByVin
      .categoryPath(getRandomString)
      .vinPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = am.create
    val sessionId = adaptor.login(account).getSession.getId

    val request = (apiClient: ApiClient) => apiClient.draft.draftByVin
      .reqSpec(defaultSpec)
      .categoryPath(CARS)
      .vinPath(VIN)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api),
      jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(CreateDraftCategoriesTest.IGNORED_PATHS: _*))
  }
}
