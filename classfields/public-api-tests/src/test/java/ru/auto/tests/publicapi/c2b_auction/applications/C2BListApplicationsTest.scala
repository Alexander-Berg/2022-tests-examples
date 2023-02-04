package ru.auto.tests.publicapi.c2b_auction.applications

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsNull
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
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals

import scala.annotation.meta.getter

@DisplayName("GET /c2b-auction/application/list")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class C2BListApplicationsTest {

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
  def findUserApplications(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId

    (0 until 10).map { _ =>
      createSomeApplication("79999999999", sessionId).getApplicationId
    }.toSet

    val resp = (client: ApiClient) =>
      client
        .application()
        .listC2BApplications()
        .reqSpec(defaultSpec)
        .pageQuery(1)
        .pageSizeQuery(10)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBe200OkJSON()))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(resp(api), jsonEquals[JsonObject](resp(prodApi)))
  }

  @Test
  def should403WhenNoAuth(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId

    api
      .application()
      .listC2BApplications()
      .pageQuery(1)
      .pageSizeQuery(10)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldReturnEmptyResponseWhenNoApplications(): Unit = {
    val account = accounts.create()
    val sessionId = adaptor.login(account).getSession.getId

    val resp = api
      .application()
      .listC2BApplications()
      .reqSpec(defaultSpec)
      .pageQuery(1)
      .pageSizeQuery(10)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON()))
      .getApplications

    MatcherAssert.assertThat(resp, IsNull.nullValue())
  }

  private def createSomeApplication(login: String, sessionId: String) = {
    val draft = adaptor.createDraftForC2B(login, sessionId)

    adaptor.createC2BApplicationFromDraft(draft.getOffer.getId, sessionId)
  }
}
