package ru.auto.tests.adaptor

import com.google.inject.{AbstractModule, Inject, Singleton}
import io.qameta.allure.Step
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200Ok, shouldBe200OkJSON, validatedWith}
import ru.auto.tests.commons.util.Utils.{getRandomShortInt, getRandomString}
import ru.auto.tests.recall.ApiClient
import ru.auto.tests.recall.model.{
  AutoApiNavigatorVehicleResponse,
  AutoApiRecallsUserCardResponse,
  AutoApiRecallsUserCardsResponse,
  RuYandexAutoVinRecallsProtoGostCampaign,
  RuYandexAutoVinRecallsProtoRecall,
  RuYandexAutoVinRecallsProtoRecallDocumentsResponse,
  RuYandexAutoVinRecallsProtoRecallResponse
}
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

@Singleton
class RecallApiAdaptor extends AbstractModule {

  @Inject
  private val api: ApiClient = null

  override protected def configure(): Unit = {}

  @Step("Получить список карточек юзера {userId}")
  def getUserCards(userId: String): AutoApiRecallsUserCardsResponse = {
    api.userCards.userCards
      .reqSpec(defaultSpec)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Создаем карточку для VIN {vin} у юзера {userId}")
  def createUserCard(vin: String, userId: String): AutoApiRecallsUserCardResponse = {
    api.userCards
      .addUserCard()
      .reqSpec(defaultSpec)
      .vinQuery(vin)
      .hasEmailQuery(false)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Подписываемся на карточку {cardId} юзером {userId}")
  def subscribeToUserCard(cardId: Long, userId: String): Unit = {
    api.userCards
      .subscribe()
      .reqSpec(defaultSpec)
      .cardIdPath(cardId)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200Ok))
  }

  @Step("Добавляем ТС от Я.Навигатора для VIN {vinCodeId} у юзера {userId}")
  def addVehicle(vinCodeId: Int, userId: String): AutoApiNavigatorVehicleResponse = {
    api.navigator
      .addVehicle()
      .reqSpec(defaultSpec)
      .vinCodeIdQuery(vinCodeId)
      .subscribeQuery(false)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Получаем отзывную компанию по {recallId} у юзера {userId}")
  def getRecall(recallId: Long, userId: String): RuYandexAutoVinRecallsProtoRecallResponse = {
    api.recalls.recall
      .reqSpec(defaultSpec)
      .recallIdPath(recallId)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Добавляем отзывную компанию для юзера {userId}")
  def addRecall(userId: String): RuYandexAutoVinRecallsProtoRecallResponse = {
    val requestBody = new RuYandexAutoVinRecallsProtoRecall()
      .title(getRandomString)
      .description(getRandomString)
      .manufacturer(getRandomString)
      .gostCampaign(
        new RuYandexAutoVinRecallsProtoGostCampaign()
          .count(getRandomShortInt)
          .marks(getRandomString)
          .models(getRandomString)
          .url(getRandomString)
      )

    api.recalls
      .addRecall()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Получаем документы из отзывной {recallId} юзера {userId}")
  def getRecallDocuments(recallId: Long, userId: String): RuYandexAutoVinRecallsProtoRecallDocumentsResponse = {
    api.recalls.recallGetDocuments
      .reqSpec(defaultSpec)
      .recallIdPath(recallId)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Добавляем документ к отзывной {recallId} юзера {userId}")
  def attachDocumentToRecall(recallId: Long, userId: String): RuYandexAutoVinRecallsProtoRecallDocumentsResponse = {
    api.recalls
      .recallAttachDocuments()
      .reqSpec(defaultSpec)
      .recallIdPath(recallId)
      .urlQuery(getRandomString)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Добавляем документ к отзывной {recallId} юзера {userId}")
  def attachDocumentToRecall(
      recallId: Long,
      userId: String,
      urlToDoc: String): RuYandexAutoVinRecallsProtoRecallDocumentsResponse = {
    api.recalls
      .recallAttachDocuments()
      .reqSpec(defaultSpec)
      .recallIdPath(recallId)
      .urlQuery(urlToDoc)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }
}
