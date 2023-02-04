package ru.auto.tests.publicapi.c2b_auction.applications

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND}
import org.hamcrest.MatcherAssert
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals

import scala.annotation.meta.getter

@DisplayName("GET /user/draft/{category}/{offerId}/c2b_application_info")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class C2BApplicationInfoTest {

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
  private val accounts: AccountManager = null

  @Test
  def shouldHaveNoDiffWithProd(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionId)

    val result = (api: ApiClient) =>
      api
        .draft()
        .loadC2BApplicationInfo()
        .reqSpec(defaultSpec)
        .categoryPath(CategoryEnum.CARS)
        .offerIdPath(draft.getOfferId)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(result(api), jsonEquals[JsonObject](result(prodApi)))
  }

  @Test
  def should403WhenNoAuth(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionId)

    api
      .draft()
      .loadC2BApplicationInfo()
      .categoryPath(CategoryEnum.CARS)
      .offerIdPath(draft.getOfferId)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def should404WhenNoDraft(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId

    api
      .draft()
      .loadC2BApplicationInfo()
      .reqSpec(defaultSpec)
      .categoryPath(CategoryEnum.CARS)
      .offerIdPath("doesnt-exist")
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }
}
