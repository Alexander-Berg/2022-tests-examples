package ru.auto.api.managers.feedback

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.auth.Application
import ru.auto.api.feedback.FeedbackModel.EmailFeedbackRequest
import ru.auto.api.managers.feedback.FeedbackManager.{FeedbackTemplateName, SupportEmail}
import ru.auto.api.model.{AutoruUser, RequestParams}
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.sender.{SenderClient, SenderParams}
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.TimeUtils.TimeProvider
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.passport.model.api.ApiModel.UserResult
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

class FeedbackManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with AsyncTasksSupport {

  trait mocks {
    val senderClient = mock[SenderClient]
    val passportClient = mock[PassportClient]
    val timeProvider = mock[TimeProvider]
    val feedbackManager = new FeedbackManager(senderClient, passportClient, timeProvider)
    implicit val trace: Traced = Traced.empty

    implicit val request: RequestImpl = {
      val req = new RequestImpl
      req.setApplication(Application.desktop)
      req.setTrace(trace)
      req.setRequestParams(RequestParams.empty)
      req
    }
    val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val dateTime = LocalDateTime.parse("2018-09-20T12:15:20.279")
    val dateString = dateTime.format(pattern)
    when(timeProvider.currentLocalDateTime()).thenReturn(dateTime)
  }

  "FeedbackManager" should {

    "send feedback email" in new mocks {
      when(passportClient.getUser(?)(?)).thenReturnF(UserResult.newBuilder().build())
      when(senderClient.sendLetter(?)(?)).thenReturnF(())
      val params = EmailFeedbackRequest
        .newBuilder()
        .setEmail("test@test.ru")
        .setMessage("body")
        .setInformation("info")
        .build()
      feedbackManager.sendFeedbackEmail(params).await
      val expectedTemplateArgs = Map(
        "date" -> dateString,
        "user_id" -> request.user.ref.toPlain,
        "message" -> "body",
        "information" -> "info"
      )
      val expectedParams = SenderParams(
        templateName = FeedbackTemplateName,
        from = Some("test@test.ru"),
        to = SupportEmail,
        templateArgs = expectedTemplateArgs
      )
      verify(senderClient).sendLetter(expectedParams)(trace)
    }

  }

  "get email from passport" in new mocks {
    request.setUser(AutoruUser(1L))
    when(passportClient.getUser(?)(?)).thenReturnF {
      val userResultBuilder = UserResult.newBuilder()
      userResultBuilder.getUserBuilder
        .addEmailsBuilder()
        .setConfirmed(true)
        .setEmail("passport-email@test.ru")
      userResultBuilder.build()
    }
    when(senderClient.sendLetter(?)(?)).thenReturnF(())

    val params = EmailFeedbackRequest
      .newBuilder()
      .setMessage("body")
      .setInformation("info")
      .build()
    feedbackManager.sendFeedbackEmail(params).await

    val expectedTemplateArgs = Map(
      "date" -> dateString,
      "user_id" -> request.user.ref.toPlain,
      "message" -> "body",
      "information" -> "info"
    )

    val expectedParams = SenderParams(
      templateName = FeedbackTemplateName,
      from = Some("passport-email@test.ru"),
      to = SupportEmail,
      templateArgs = expectedTemplateArgs
    )
    verify(senderClient).sendLetter(expectedParams)(trace)
  }

}
