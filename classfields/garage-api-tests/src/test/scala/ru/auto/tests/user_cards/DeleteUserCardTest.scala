package ru.auto.tests.user_cards

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.Base
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.{getRandomShortLong, getRandomString}
import ru.auto.tests.constants.Owners.CARFAX
import ru.auto.tests.garage.ApiClient
import ru.auto.tests.garage.model._
import ru.auto.tests.module.GarageApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("DELETE /user/card/{card-id}")
@GuiceModules(Array(classOf[GarageApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteUserCardTest extends Base {

  private val userId = "qa_user:64617860"
  private val vin = "KMHDN41BP6U366989"
  private val cardId = 2146855701

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(CARFAX)
  def shouldSee400WithEmptyBody(): Unit = {
    api.userCard
      .deleteCard()
      .reqSpec(defaultSpec)
      .cardIdPath(cardId)
      .xUserIdHeader(s"user:$getRandomShortLong")
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(CARFAX)
  def shouldDeleteCard(): Unit = {
    val builtCard = adaptor.buildCardFromVin(userId, vin, "CURRENT_CAR")

    val requestBody = new AutoApiVinGarageCreateCardRequest()
      .addedManually(false)
      .addedByIdentifier(AutoApiVinGarageCreateCardRequest.AddedByIdentifierEnum.VIN)
      .card(builtCard.getCard)

    val createResponse = adaptor.createCard(userId, requestBody)

    api.userCard
      .deleteCard()
      .reqSpec(defaultSpec)
      .cardIdPath(createResponse.getCard.getId)
      .xUserIdHeader(userId)
      .execute(validatedWith(shouldBe200OkJSON))
  }

  @Test
  @Owner(CARFAX)
  def shouldThrowNotFoundWhenDeleteNotOwnCard(): Unit = {
    val user = genUser()
    val builtCard = adaptor.buildCardFromVin(user, vin, "CURRENT_CAR")

    val requestBody = new AutoApiVinGarageCreateCardRequest()
      .addedManually(false)
      .addedByIdentifier(AutoApiVinGarageCreateCardRequest.AddedByIdentifierEnum.VIN)
      .card(builtCard.getCard)

    val createResponse = adaptor.createCard(user, requestBody)

    val result = api.userCard
      .deleteCard()
      .reqSpec(defaultSpec)
      .cardIdPath(createResponse.getCard.getId)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBeCode(SC_NOT_FOUND)))

    assert(result.getStatus == AutoApiVinGarageDeleteCardResponse.StatusEnum.ERROR)
    assert(result.getError == AutoApiVinGarageDeleteCardResponse.ErrorEnum.CARD_NOT_FOUND)
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400WithInvalidUser(): Unit = {
    api.userCard
      .deleteCard()
      .reqSpec(defaultSpec)
      .cardIdPath(cardId)
      .xUserIdHeader(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400WithInvalidCardId(): Unit = {
    api.userCard
      .deleteCard()
      .reqSpec(defaultSpec)
      .cardIdPath(getRandomString)
      .xUserIdHeader(userId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

}
