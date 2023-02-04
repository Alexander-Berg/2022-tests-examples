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

@DisplayName("POST /garage/card/{cardId}/report/raw")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class BuyAndGetReportTest {

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

    adaptor.buyVinHistory(sessionId, session.getUserId, VinWithReport)

    val buyAndGetReport = (api: ApiClient, cardId: String) =>
      api
        .garage()
        .buyAndGetReport()
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

    val cardId = adaptor.createGarageCardByVinOrLp(VinWithReport, session.getId).getCard.getId
    val actual: JsonObject = buyAndGetReport(api, cardId)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(cardId, sessionId)

    val prodCardId = adaptor.createGarageCardByVinOrLp(VinWithReport, session.getId).getCard.getId
    val expected: JsonObject = buyAndGetReport(prodApi, prodCardId)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(prodCardId, sessionId)
    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(RawReportUtils.IGNORED_PATHS: _*)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProductionWhenNotBought(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId

    val buyAndGetReport = (api: ApiClient, cardId: String) =>
      api
        .garage()
        .buyAndGetReport()
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

    val cardId = adaptor.createGarageCardByVinOrLp(VinWithReport, session.getId).getCard.getId
    val actual: JsonObject = buyAndGetReport(api, cardId)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(cardId, sessionId)

    val prodCardId = adaptor.createGarageCardByVinOrLp(VinWithReport, session.getId).getCard.getId
    val expected: JsonObject = buyAndGetReport(prodApi, prodCardId)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(prodCardId, sessionId)
    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(RawReportUtils.IGNORED_PATHS: _*)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetCardNotFoundError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId

    val buyAndGetReport: ApiClient => String = (api: ApiClient) =>
      api
        .garage()
        .buyAndGetReport()
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

    val actual: String = buyAndGetReport(api)
    val matchExpected: Matcher[String] = equalTo(buyAndGetReport(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetCardNotFoundErrorWhenNotAuthenticated(): Unit = {
    val buyAndGetReport: ApiClient => String = (api: ApiClient) =>
      api
        .garage()
        .buyAndGetReport()
        .cardIdPath(123)
        .reqSpec(defaultSpec())
        .execute(
          validatedWith(
            shouldBeCode(SC_NOT_FOUND)
              .expectContentType(ContentType.TEXT)
          )
        )
        .as(classOf[String])

    val actual: String = buyAndGetReport(api)
    val matchExpected: Matcher[String] = equalTo(buyAndGetReport(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetVinNotFoundError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId
    val bodyJson = getResourceAsString("garage/create_card_request_with_unknown_vin.json")

    val buyAndGetReport = (api: ApiClient, cardId: String) =>
      api
        .garage()
        .buyAndGetReport()
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

    val cardId = adaptor.createGarageCard(bodyJson, sessionId).getCard.getId
    val actual: String = buyAndGetReport(api, cardId)
    adaptor.deleteGarageCard(cardId, sessionId)

    val prodCardId = adaptor.createGarageCard(bodyJson, sessionId).getCard.getId
    val expected: String = buyAndGetReport(prodApi, prodCardId)
    adaptor.deleteGarageCard(prodCardId, sessionId)
    val matchExpected: Matcher[String] = equalTo(expected)

    MatcherAssert.assertThat(actual, matchExpected)
  }
}
