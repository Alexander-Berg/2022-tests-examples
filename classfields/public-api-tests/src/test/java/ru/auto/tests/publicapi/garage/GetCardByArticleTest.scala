package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.hamcrest.{Matcher, MatcherAssert}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.utils.UtilsPublicApi
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("GET /garage/user/card/article/{article_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCardByArticleTest {

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

  @Inject private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId

    val articleId = "kiacerato"

    val getCard: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .buildCardByArticleId()
        .xSessionIdHeader(sessionId)
        .articleIdPath(articleId)
        .reqSpec(defaultSpec())
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])

    val actual: JsonObject = getCard(api)

    val expected: JsonObject = getCard(prodApi)

    val matchExpected: Matcher[JsonObject] = jsonEquals(expected)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetNotFoundError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession

    val articleId = UtilsPublicApi.getRandomArticleId

    val createCard: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .buildCardByArticleId()
        .reqSpec(defaultSpec())
        .xSessionIdHeader(session.getId)
        .articleIdPath(articleId)
        .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
        .as(classOf[JsonObject])

    val actual: JsonObject = createCard(api)
    val matchExpected: Matcher[JsonObject] = jsonEquals(createCard(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }

}