package ru.auto.tests.user_cards

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND, SC_OK}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.Base
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.constants.Owners.CARFAX
import ru.auto.tests.garage.ApiClient
import ru.auto.tests.garage.model.AutoApiVinGarageCardTypeInfo.{CardTypeEnum => CardType}
import ru.auto.tests.garage.model.AutoApiVinGarageChangeCardTypeRequest.CardTypeEnum
import ru.auto.tests.garage.model.{AutoApiVinGarageChangeCardTypeRequest, AutoApiVinGarageCreateCardRequest}
import ru.auto.tests.module.GarageApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("PUT /user/card/{card-id}/change_type")
@GuiceModules(Array(classOf[GarageApiModule]))
@RunWith(classOf[GuiceTestRunner])
class ChangeCardTypeTest extends Base {

  private val userId = "qa_user:64617860"
  private val vin = "KMHDN41BP6U366989"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(CARFAX)
  def shouldSee404ForUnknownCard(): Unit = {
    api.userCard
      .changeCardType()
      .reqSpec(defaultSpec)
      .cardIdPath(-1)
      .xUserIdHeader(userId)
      .body(new AutoApiVinGarageChangeCardTypeRequest().cardType(CardTypeEnum.EX_CAR))
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400ForUnknownCardType(): Unit = {
    val userId = genUser()
    val builtCard = adaptor.buildCardFromVin(userId, vin, "CURRENT_CAR")

    val requestBody = new AutoApiVinGarageCreateCardRequest()
      .addedManually(false)
      .addedByIdentifier(AutoApiVinGarageCreateCardRequest.AddedByIdentifierEnum.VIN)
      .card(builtCard.getCard)

    val createResponse = adaptor.createCard(userId, requestBody)

    api.userCard
      .changeCardType()
      .reqSpec(defaultSpec)
      .cardIdPath(createResponse.getCard.getId)
      .xUserIdHeader(userId)
      .body(new AutoApiVinGarageChangeCardTypeRequest().cardType(CardTypeEnum.UNKNOWN_CARD_TYPE))
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(CARFAX)
  def shouldChangeCardType(): Unit = {
    val userId = genUser()
    val builtCard = adaptor.buildCardFromVin(userId, vin, "CURRENT_CAR")

    val requestBody = new AutoApiVinGarageCreateCardRequest()
      .addedManually(false)
      .addedByIdentifier(AutoApiVinGarageCreateCardRequest.AddedByIdentifierEnum.VIN)
      .card(builtCard.getCard)

    val createResponse = adaptor.createCard(userId, requestBody)
    val cardId = createResponse.getCard.getId

    api.userCard
      .changeCardType()
      .reqSpec(defaultSpec)
      .cardIdPath(cardId)
      .xUserIdHeader(userId)
      .body(new AutoApiVinGarageChangeCardTypeRequest().cardType(CardTypeEnum.EX_CAR))
      .execute(validatedWith(shouldBeCode(SC_OK)))

    val request = (apiClient: ApiClient) =>
      apiClient.userCard.getCard
        .reqSpec(defaultSpec)
        .cardIdPath(cardId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi))
    )

    val updatedCard = api.userCard.getCard
      .reqSpec(defaultSpec)
      .cardIdPath(cardId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assert(updatedCard.getCard.getCardTypeInfo.getCardType == CardType.EX_CAR)

  }

}
