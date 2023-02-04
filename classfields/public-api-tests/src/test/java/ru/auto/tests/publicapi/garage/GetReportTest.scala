package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import io.restassured.http.ContentType
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.{Matcher, MatcherAssert}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.util.Utils.getResourceAsString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.carfax.RawReportUtils
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("GET /garage/card/{cardId}/report/raw")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetReportTest {

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

  private val VinWithReport = "X9FLXXEEBLES67719"

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProductionWhenBought(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId

    val cardId = adaptor.createGarageCardByVinOrLp(VinWithReport, sessionId).getCard.getId

    adaptor.buyVinHistory(sessionId, session.getUserId, VinWithReport)

    val getReport = (api: ApiClient, cardId: String) =>
      api
        .garage()
        .getReport
        .xSessionIdHeader(sessionId)
        .cardIdPath(cardId)
        .reqSpec(defaultSpec())
        .execute(
          validatedWith(
            shouldBeSuccess()
              .expectContentType(ContentType.JSON)
          )
        )
        .as(classOf[JsonObject])

    val actual: JsonObject = getReport(api, cardId)
    val expected: JsonObject = getReport(prodApi, cardId)
    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(RawReportUtils.IGNORED_PATHS: _*)

    adaptor.deleteGarageCardAndWaitRecallsDeleted(cardId, sessionId)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProductionWhenNotBought(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId

    val cardId = adaptor.createGarageCardByVinOrLp(VinWithReport, sessionId).getCard.getId

    val getReport = (api: ApiClient, cardId: String) =>
      api
        .garage()
        .getReport
        .xSessionIdHeader(sessionId)
        .cardIdPath(cardId)
        .reqSpec(defaultSpec())
        .execute(
          validatedWith(
            shouldBeSuccess()
              .expectContentType(ContentType.JSON)
          )
        )
        .as(classOf[JsonObject])

    val actual: JsonObject = getReport(api, cardId)
    val expected: JsonObject = getReport(prodApi, cardId)
    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(RawReportUtils.IGNORED_PATHS: _*)

    adaptor.deleteGarageCardAndWaitRecallsDeleted(cardId, sessionId)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetCardNotFoundError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId

    val getReport: ApiClient => String = (api: ApiClient) =>
      api
        .garage()
        .getReport
        .xSessionIdHeader(sessionId)
        .cardIdPath(123)
        .reqSpec(defaultSpec())
        .execute(
          validatedWith(
            shouldBeCode(SC_NOT_FOUND)
              .expectContentType(ContentType.TEXT)
          )
        )
        .as(classOf[String])

    val actual: String = getReport(api)
    val matchExpected: Matcher[String] = equalTo(getReport(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetCardNotFoundErrorWhenNotAuthenticated(): Unit = {
    val getReport: ApiClient => String = (api: ApiClient) =>
      api
        .garage()
        .getReport
        .cardIdPath(123)
        .reqSpec(defaultSpec())
        .execute(
          validatedWith(
            shouldBeCode(SC_NOT_FOUND)
              .expectContentType(ContentType.TEXT)
          )
        )
        .as(classOf[String])

    val actual: String = getReport(api)
    val matchExpected: Matcher[String] = equalTo(getReport(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetVinNotFoundError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId
    val bodyJson = getResourceAsString("garage/create_card_request_with_unknown_vin.json")

    val cardId = adaptor.createGarageCard(bodyJson, sessionId).getCard.getId

    val getReport = (api: ApiClient, cardId: String) =>
      api
        .garage()
        .getReport
        .xSessionIdHeader(sessionId)
        .cardIdPath(cardId)
        .reqSpec(defaultSpec())
        .execute(
          validatedWith(
            shouldBeCode(SC_BAD_REQUEST)
              .expectContentType(ContentType.TEXT)
          )
        )
        .as(classOf[String])

    val actual: String = getReport(api, cardId)
    val matchExpected: Matcher[String] = equalTo(getReport(prodApi, cardId))
    adaptor.deleteGarageCard(cardId, sessionId)

    MatcherAssert.assertThat(actual, matchExpected)
  }
}
