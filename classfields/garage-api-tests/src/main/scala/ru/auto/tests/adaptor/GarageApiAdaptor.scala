package ru.auto.tests.adaptor

import com.google.inject.{AbstractModule, Inject, Singleton}
import io.qameta.allure.Step
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, validatedWith}
import ru.auto.tests.garage.ApiClient
import ru.auto.tests.garage.model._
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

@Singleton
class GarageApiAdaptor extends AbstractModule {

  @Inject
  private val api: ApiClient = null

  override protected def configure(): Unit = {}

  @Step("Получить карточку по айди {cardId}")
  def getUserCard(cardId: String): AutoApiVinGarageGetCardResponse = {
    api.userCard.getCard
      .cardIdPath(cardId)
      .reqSpec(defaultSpec)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Создаем карточку для юзера {userId}")
  def createCard(userId: String, request: AutoApiVinGarageCreateCardRequest): AutoApiVinGarageCreateCardResponse = {

    api.userCard
      .createCard()
      .reqSpec(defaultSpec)
      .body(request)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Собираем карточку для юзера {userId} по vin {vin}")
  def buildCardFromVin(userId: String, vin: String, cardType: String): AutoApiVinGarageCreateCardResponse = {
    api.userCard
      .buildCardFromVin()
      .reqSpec(defaultSpec)
      .xUserIdHeader(userId)
      .vinQuery(vin)
      .cardTypeQuery(cardType)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Обновляем карточку {cardId} юзером {userId}")
  def updateCard(cardId: Long, userId: String, card: AutoApiVinGarageCard): AutoApiVinGarageUpdateCardResponse = {
    val requestBody = new AutoApiVinGarageUpdateCardRequest().card(card)
    api.userCard
      .updateCard()
      .reqSpec(defaultSpec)
      .cardIdPath(cardId)
      .body(requestBody)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Удаляем карточку {cardId} юзером {userId}")
  def deleteCard(cardId: Long, userId: String): AutoApiVinGarageDeleteCardResponse = {
    api.userCard
      .deleteCard()
      .reqSpec(defaultSpec)
      .cardIdPath(cardId)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Получение всех активных карточек юзера {userId}")
  def getCards(userId: String): AutoApiVinGarageGetListingResponse = {
    api
      .batchUserCards()
      .getUserCards
      .reqSpec(defaultSpec)
      .xUserIdHeader(userId)
      .body(
        new AutoApiVinGarageGetListingRequest()
          .filters(
            new AutoApiVinGarageGetListingRequestFilters()
              .addStatusItem(AutoApiVinGarageGetListingRequestFilters.StatusEnum.ACTIVE)
          )
      )
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

}
