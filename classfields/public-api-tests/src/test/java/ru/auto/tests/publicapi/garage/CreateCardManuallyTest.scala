package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
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
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.{defaultSpec, withJsonBody}
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("POST /garage/user/card")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateCardManuallyTest {

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

  private val createCard = (api: ApiClient, sessionId: String, bodyJson: String) =>
    api
      .garage()
      .createCard()
      .xSessionIdHeader(sessionId)
      .reqSpec(defaultSpec())
      .reqSpec(withJsonBody(bodyJson))
      .execute(validatedWith(shouldBeSuccess()))
      .as(classOf[JsonObject])

  private val getCardId = (json: JsonObject) => json.getAsJsonObject("card").get("id").getAsString

  @Test
  @Owner(CARFAX)
  def shouldHaveNoDiffWithProductionForTheCurrentCard(): Unit = {
    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId
    val bodyJson = getResourceAsString("garage/create_card_request.json")

    val actual: JsonObject = createCard(api, sessionId, bodyJson)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(getCardId(actual), sessionId)

    val expected: JsonObject = createCard(prodApi, sessionId, bodyJson)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(getCardId(expected), sessionId)
    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(GarageTestUtils.ignoredPaths: _*)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldHaveNoDiffWithProductionForTheDreamCard(): Unit = {
    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId
    val bodyJson = getResourceAsString("garage/create_dream_card_request.json")

    val actual: JsonObject = createCard(api, sessionId, bodyJson)
    adaptor.deleteGarageCard(getCardId(actual), sessionId)

    val expected: JsonObject = createCard(prodApi, sessionId, bodyJson)
    adaptor.deleteGarageCard(getCardId(expected), sessionId)
    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(GarageTestUtils.ignoredPaths: _*)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetValidationError(): Unit = {
    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId
    val bodyJson = getResourceAsString("garage/invalid_create_card_request.json")

    val createCard: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .createCard()
        .xSessionIdHeader(sessionId)
        .reqSpec(defaultSpec())
        .reqSpec(withJsonBody(bodyJson))
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
        .as(classOf[JsonObject])

    val actual: JsonObject = createCard(api)
    val matchExpected: Matcher[JsonObject] = jsonEquals(createCard(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetVinCodeInvalidError(): Unit = {
    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId
    val bodyJson = getResourceAsString("garage/create_card_request_with_invalid_vin.json")

    val createCard: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .createCard()
        .xSessionIdHeader(sessionId)
        .reqSpec(defaultSpec())
        .reqSpec(withJsonBody(bodyJson))
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
        .as(classOf[JsonObject])

    val actual: JsonObject = createCard(api)
    val matchExpected: Matcher[JsonObject] = jsonEquals(createCard(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }
}
