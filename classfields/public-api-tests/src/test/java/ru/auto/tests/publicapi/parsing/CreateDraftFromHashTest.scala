package ru.auto.tests.publicapi.parsing

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
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
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /parsing/{hash}/to-draft")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateDraftFromHashTest {

  private val HASH = "ab1d3b8dfdc5f04a3c09a5b78effc5fb"
  private val IGNORED_PATHS = Array("offer_id", "offer.id", "offer.state.upload_url", "offer.url", "offer.mobile_url",
    "offer.price_history[*].create_timestamp", "offer.price_info.create_timestamp", "offer.additional_info.expire_date",
    "offer.additional_info.actualize_date", "offer.additional_info.creation_date", "offer.additional_info.update_date",
    "offer.created", "offer.state.document_photo_upload_urls.driving_license", "offer.state.sts_upload_url",
    "offer.state.document_photo_upload_urls.sts_back", "offer.state.document_photo_upload_urls.sts_front")

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
    api.parsing.createDraftFromParsedOffer()
      .hashPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidParsingHash(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    api.parsing.createDraftFromParsedOffer().reqSpec(defaultSpec)
      .hashPath(getRandomString)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee403WithoutSessionId(): Unit = {
    val response = api.parsing.createDraftFromParsedOffer().reqSpec(defaultSpec)
      .hashPath(HASH)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NO_AUTH).hasDetailedError("Need authentication")
  }

  @Test
  @Owner(TIMONDL)
  def shouldCreateDraftFromParsingHash(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val response = api.parsing.createDraftFromParsedOffer().reqSpec(defaultSpec)
      .hashPath(HASH)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    Assertions.assertThat(response.getOffer.getId).isNotBlank
  }

  @Test
  @Owner(TIMONDL)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val req = (apiClient: ApiClient) => apiClient.parsing.createDraftFromParsedOffer()
      .reqSpec(defaultSpec)
      .hashPath(HASH)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi)).whenIgnoringPaths(IGNORED_PATHS: _*))
  }
}
