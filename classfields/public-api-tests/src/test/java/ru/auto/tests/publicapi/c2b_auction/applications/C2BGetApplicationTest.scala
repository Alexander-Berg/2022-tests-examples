package ru.auto.tests.publicapi.c2b_auction.applications

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND}
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.anno.Prod

import scala.annotation.meta.getter

@DisplayName("GET /c2b-auction/application/{applicationId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class C2BGetApplicationTest {

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
  def shouldGetMyApplication(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionId)
    val application = adaptor.createC2BApplicationFromDraft(draft.getOfferId, sessionId)

    val resp = (client: ApiClient) =>
      client
        .application()
        .getC2BApplication
        .reqSpec(defaultSpec)
        .applicationIdPath(application.getApplicationId)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBe200OkJSON()))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(resp(api), jsonEquals[JsonObject](resp(prodApi)))
  }

  @Test
  def should403WhenNotOwn(): Unit = {
    val accountCreate = accounts.create()
    val sessionCreate = adaptor.login(accountCreate).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionCreate)
    val application = adaptor.createC2BApplicationFromDraft(draft.getOfferId, sessionCreate)

    val accountGet = accounts.create()
    val sessionGet = adaptor.login(accountGet).getSession.getId

    api
      .application()
      .getC2BApplication
      .reqSpec(defaultSpec)
      .applicationIdPath(application.getApplicationId)
      .xSessionIdHeader(sessionGet)
      .executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def should403WhenNoAuth(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId
    val draft = adaptor.createDraftForC2B("79999999999", sessionId)
    val application = adaptor.createC2BApplicationFromDraft(draft.getOfferId, sessionId)

    api
      .application()
      .getC2BApplication
      .applicationIdPath(application.getApplicationId)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def should404WhenNoApplication(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId

    api
      .application()
      .getC2BApplication
      .reqSpec(defaultSpec)
      .applicationIdPath(Long.MaxValue - 1)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }
}
