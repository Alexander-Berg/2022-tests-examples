package ru.auto.chatbot.manager

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.JsValue
import ru.auto.chatbot.app.TestContext
import ru.auto.chatbot.basic_model.OfferInfo
import ru.auto.chatbot.client.SenderClient
import ru.auto.chatbot.state_model.{QuestionAnswer, State}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-16.
  */
class SenderManagerTest extends FunSuite with MockitoSupport with BeforeAndAfter with Matchers with ScalaFutures {

  import TestContext._

  test("send letter") {
    val title = "title"
    val offerUrl = "url"
    val comment = "comment"
    val imageUrl = "img_url"
    val email = "email"
    val price = 99999.0f
    val questionAnswers = (1 to 24).map(n => n.toString -> QuestionAnswer("question", "OK")).toMap
    val openQuestionsAnswer = (1 to 2).map(n => n.toString -> "ответ").toMap

    val senderClient = mock[SenderClient]
    when(senderClient.sendLetter(?, ?, ?)).thenReturn(Future.unit)

    val senderManager = new SenderManager(senderClient, stateWrapper)
    val state = State(
      questionAnswers = questionAnswers,
      openQuestionAnswers = openQuestionsAnswer,
      offerTitle = title,
      offerUrl = offerUrl,
      offerMainImageUrl = imageUrl,
      comment = comment,
      userPassportEmail = email,
      offerPrice = price
    )

    val captor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

    Await.result(senderManager.sendSummaryLetter(state, "test"), 10.seconds)

    verify(senderClient).sendLetter(eq("test"), eq(email), captor.capture())

    val sentLetter = captor.getValue

    println(sentLetter)

    val offerInfo = OfferInfo(
      title = title,
      image = imageUrl,
      link = offerUrl,
      price = price.toString
    )

    val closeQuestions = questionAnswers.map { case (_, answer) => }

  }

}
