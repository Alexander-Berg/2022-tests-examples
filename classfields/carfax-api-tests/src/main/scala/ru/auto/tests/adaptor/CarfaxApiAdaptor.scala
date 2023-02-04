package ru.auto.tests.adaptor

import com.google.inject.{AbstractModule, Inject, Singleton}
import io.qameta.allure.Step
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200Ok, shouldBe200OkJSON}
import ru.auto.tests.commons.util.Utils.{getRandomShortInt, getRandomString}
import ru.auto.tests.model.AutoApiVinConfirmedKmAge.EventTypeEnum
import ru.auto.tests.model.{
  AutoApiVinCommentsUser,
  AutoApiVinCommentsVinReportComment,
  AutoApiVinCommentsVinReportCommentAddResponse,
  AutoApiVinConfirmedKmAge
}
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.util.Random

@Singleton
class CarfaxApiAdaptor extends AbstractModule {

  @Inject
  private val api: ApiClient = null

  @Step("Создаем комментария для отчета {vin}, блок {blockId}")
  def createComment(vin: String, blockId: String, userId: String): AutoApiVinCommentsVinReportCommentAddResponse = {
    val body = new AutoApiVinCommentsVinReportComment()
      .text(getRandomString)
      .blockId(blockId)
      .user(new AutoApiVinCommentsUser().id(userId).name(getRandomString))

    api.comments
      .addComment()
      .reqSpec(defaultSpec)
      .vinPath(vin)
      .body(body)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }

  @Step("Обновляем пробег в отчете {vin} для события {eventType} {id}")
  def updateMillageInReport(vin: String, eventType: EventTypeEnum, id: String): Unit = {
    api.moderation
      .updateKmAge()
      .reqSpec(defaultSpec)
      .vinQuery(vin)
      .body(new AutoApiVinConfirmedKmAge().eventType(eventType).id(id).value(Random.between(1, 100000)))
      .xUserIdHeader(s"qa_user:$getRandomShortInt")
      .execute(validatedWith(shouldBe200Ok))
  }

  override protected def configure(): Unit = {}

}
