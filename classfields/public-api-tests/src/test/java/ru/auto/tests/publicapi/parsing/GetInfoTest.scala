package ru.auto.tests.publicapi.parsing

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /parsing/{hash}/info")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetInfoTest {

  private val HASH = "ab1d3b8dfdc5f04a3c09a5b78effc5fb"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.parsing.getParsedOfferInfo
      .hashPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidParsingHash(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    api.parsing.getParsedOfferInfo.reqSpec(defaultSpec)
      .hashPath(getRandomString)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val req = (apiClient: ApiClient) => apiClient.parsing.getParsedOfferInfo
      .reqSpec(defaultSpec)
      .hashPath(HASH)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
