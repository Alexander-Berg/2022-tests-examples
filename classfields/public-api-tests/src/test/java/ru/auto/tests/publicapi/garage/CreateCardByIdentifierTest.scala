package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}
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

@DisplayName("POST /garage/user/card/identifier/{cardId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateCardByIdentifierTest {

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

  private val getCardId = (json: JsonObject) => json.getAsJsonObject("card").get("id").getAsString

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProductionForTheCurrentCard(): Unit = {
    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId

    val createCard: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .createCardFromIdentifier()
        .xSessionIdHeader(sessionId)
        .identifierPath("KMHDN41BP6U366989")
        .reqSpec(defaultSpec())
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])

    val actual: JsonObject = createCard(api)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(getCardId(actual), sessionId)

    val expected: JsonObject = createCard(prodApi)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(getCardId(expected), sessionId)
    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(GarageTestUtils.ignoredPaths: _*)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProductionForTheExCard(): Unit = {
    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId
    val jsonBody = getResourceAsString("garage/create_ex_card_for_identifier_request.json")

    val createCard: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .createCardFromIdentifier()
        .xSessionIdHeader(sessionId)
        .identifierPath("KMHDN41BP6U366989")
        .reqSpec(defaultSpec())
        .reqSpec(withJsonBody(jsonBody))
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])

    val actual: JsonObject = createCard(api)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(getCardId(actual), sessionId)

    val expected: JsonObject = createCard(prodApi)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(getCardId(expected), sessionId)
    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(GarageTestUtils.ignoredPaths: _*)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetValidationError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId

    val createCard: ApiClient => JsonObject = (api: ApiClient) => {
      api
        .garage()
        .createCardFromIdentifier()
        .reqSpec(defaultSpec())
        .xSessionIdHeader(sessionId)
        .identifierPath("X9FLXXEEBLES6771")
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
        .as(classOf[JsonObject])
    }

    val actual: JsonObject = createCard(api)
    val matchExpected: Matcher[JsonObject] = jsonEquals(createCard(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetValidationErrorForTheDreamCard(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val sessionId = session.getId
    val jsonBody = getResourceAsString("garage/create_dream_card_for_identifier_request.json")

    val createCard: ApiClient => JsonObject = (api: ApiClient) => {
      api
        .garage()
        .createCardFromIdentifier()
        .reqSpec(defaultSpec())
        .xSessionIdHeader(sessionId)
        .identifierPath("KMHDN41BP6U366989")
        .reqSpec(withJsonBody(jsonBody))
        .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
        .as(classOf[JsonObject])
    }

    val actual: JsonObject = createCard(api)
    val matchExpected: Matcher[JsonObject] = jsonEquals(createCard(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetNotFoundError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession

    val createCard: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .createCardFromIdentifier()
        .reqSpec(defaultSpec())
        .xSessionIdHeader(session.getId)
        .identifierPath("Z0NZWE00054341404")
        .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
        .as(classOf[JsonObject])

    val actual: JsonObject = createCard(api)
    val matchExpected: Matcher[JsonObject] = jsonEquals(createCard(prodApi))

    MatcherAssert.assertThat(actual, matchExpected)
  }
}
