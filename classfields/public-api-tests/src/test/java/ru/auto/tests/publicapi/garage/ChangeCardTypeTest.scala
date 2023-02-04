package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
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

@DisplayName("POST /garage/user/card/{identifier}/change_type")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class ChangeCardTypeTest {

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

  private val getCardType = (json: JsonObject) =>
    json
      .getAsJsonObject("card")
      .getAsJsonObject("card_type_info")
      .get("card_type")
      .getAsString

  private val getCardById = (api: ApiClient, sessionId: String, cardId: String) =>
    api
      .garage()
      .getCard
      .xSessionIdHeader(sessionId)
      .reqSpec(defaultSpec())
      .cardIdPath(cardId)
      .execute(validatedWith(shouldBeSuccess()))
      .as(classOf[JsonObject])

  private val createCard = (apiClient: ApiClient, sessionId: String, body: String) =>
    apiClient
      .garage()
      .createCard()
      .xSessionIdHeader(sessionId)
      .reqSpec(defaultSpec())
      .reqSpec(withJsonBody(body))
      .execute(validatedWith(shouldBeSuccess()))
      .as(classOf[JsonObject])

  private val changeCardType =
    (api: ApiClient, sessionId: String, cardId: String, body: String) =>
      api
        .garage()
        .changeCardType()
        .cardIdPath()
        .xSessionIdHeader(sessionId)
        .cardIdPath(cardId)
        .reqSpec(defaultSpec())
        .reqSpec(withJsonBody(body))
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId

    val createCardJson = getResourceAsString("garage/create_card_request.json")

    val changeTypeToExJson = getResourceAsString("garage/change_card_to_ex_request.json")
    val changeTypeToCurrentJson = getResourceAsString("garage/change_card_to_current_request.json")

    val typesTransition = List(
      changeTypeToExJson,
      changeTypeToCurrentJson,
      changeTypeToExJson
    )

    val changeTypesWithSweeping = (apiClient: ApiClient) => {
      val card = createCard(apiClient, sessionId, createCardJson)
      val cardId = getCardId(card)
      typesTransition.foreach(newType => {
        changeCardType(apiClient, sessionId, cardId, newType)
      })
      val updatedCard = getCardById(apiClient, sessionId, cardId)
      adaptor.deleteGarageCardAndWaitRecallsDeleted(cardId, sessionId)
      updatedCard
    }

    val actual = changeTypesWithSweeping(api)
    val expected = changeTypesWithSweeping(prodApi)

    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(GarageTestUtils.ignoredPaths: _*)
    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldAnswerWithBadRequest(): Unit = {
    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId

    val createCardJson = getResourceAsString("garage/create_card_request.json")

    val changeTypeToDreamJson = getResourceAsString("garage/change_card_to_dream_request.json")

    val card = adaptor.createGarageCard(createCardJson, sessionId)
    val cardId = card.getCard.getId
    val changeType = (api: ApiClient) =>
      api
        .garage()
        .changeCardType()
        .cardIdPath()
        .xSessionIdHeader(sessionId)
        .cardIdPath(cardId)
        .reqSpec(defaultSpec())
        .reqSpec(withJsonBody(changeTypeToDreamJson))
        .execute(validatedWith(shouldBeCode(400)))
        .as(classOf[JsonObject])

    val actual = changeType(api)
    val expected = changeType(prodApi)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(cardId, sessionId)

    val matchExpected: Matcher[JsonObject] = jsonEquals(expected).whenIgnoringPaths(GarageTestUtils.ignoredPaths: _*)
    MatcherAssert.assertThat(actual, matchExpected)
  }

}
