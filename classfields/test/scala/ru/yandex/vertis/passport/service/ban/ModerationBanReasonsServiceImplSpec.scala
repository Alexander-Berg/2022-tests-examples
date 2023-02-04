package ru.yandex.vertis.passport.service.ban

import org.joda.time.{DateTime, DateTimeUtils}
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, WordSpec}
import play.api.libs.json.Json
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
import ru.yandex.vertis.passport.integration.bunker.BunkerClient
import ru.yandex.vertis.passport.test.SpecBase

import scala.concurrent.Future

class ModerationBanReasonsServiceImplSpec extends WordSpec with SpecBase with MockitoSupport with BeforeAndAfter {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val reason = "reason1"

  private val startTime = new DateTime(2021, 1, 12, 0, 0, 0, 0)
  private val after4Min = startTime.plusMinutes(4)
  private val after5Min = startTime.plusMinutes(6)
  private val after20Min = startTime.plusMinutes(20)
  private val after30Min = startTime.plusMinutes(31)

  class Context {
    val bunkerClient: BunkerClient = mock[BunkerClient]
    val banReasonsProvider = new ModerationBanReasonsServiceImpl(bunkerClient)
  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

  "ModerationBanReasonsServiceImpl" should {
    "go to bunker and cache new reason" in new Context {
      DateTimeUtils.setCurrentMillisFixed(startTime.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future.successful(Some(Json.obj("text_user_ban" -> "text"))))
      val result0 = banReasonsProvider.getReasonDescription(reason).futureValue
      result0 should be(defined)
      result0.value.textUserBan should be(defined)
      result0.value.textUserBan.value shouldBe "text"
      verify(bunkerClient).getNode(eeq(List("vertis-moderation", "autoru", "reasons", reason)))(?)
    }

    "return from cache if value not expired" in new Context {
      DateTimeUtils.setCurrentMillisFixed(startTime.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future.successful(Some(Json.obj("text_user_ban" -> "text"))))
      banReasonsProvider.getReasonDescription(reason).futureValue
      verify(bunkerClient).getNode(eeq(List("vertis-moderation", "autoru", "reasons", reason)))(?)
      DateTimeUtils.setCurrentMillisFixed(after20Min.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future.successful(Some(Json.obj("text_user_ban" -> "text2"))))
      val result = banReasonsProvider.getReasonDescription(reason).futureValue
      verifyNoMoreInteractions(bunkerClient)
      result.value.textUserBan.value shouldBe "text"
    }

    "recache reason after 30 minutes" in new Context {
      DateTimeUtils.setCurrentMillisFixed(startTime.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future.successful(Some(Json.obj("text_user_ban" -> "text"))))
      banReasonsProvider.getReasonDescription(reason).futureValue
      DateTimeUtils.setCurrentMillisFixed(after30Min.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future.successful(Some(Json.obj("text_user_ban" -> "text2"))))
      val result = banReasonsProvider.getReasonDescription(reason).futureValue
      verify(bunkerClient, times(2)).getNode(eeq(List("vertis-moderation", "autoru", "reasons", reason)))(?)
      result.value.textUserBan.value shouldBe "text2"
    }

    "not recache if recache underway" in new Context {
      DateTimeUtils.setCurrentMillisFixed(startTime.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future.successful(Some(Json.obj("text_user_ban" -> "text"))))
      banReasonsProvider.getReasonDescription(reason).futureValue
      DateTimeUtils.setCurrentMillisFixed(after30Min.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future {
        Thread.sleep(1000); Some(Json.obj("text_user_ban" -> "text2"))
      })
      banReasonsProvider.getReasonDescription(reason)
      val result = banReasonsProvider.getReasonDescription(reason).futureValue
      verify(bunkerClient, times(2)).getNode(eeq(List("vertis-moderation", "autoru", "reasons", reason)))(?)
      result.value.textUserBan.value shouldBe "text"
    }

    "return previous value on failure" in new Context {
      DateTimeUtils.setCurrentMillisFixed(startTime.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future.successful(Some(Json.obj("text_user_ban" -> "text"))))
      banReasonsProvider.getReasonDescription(reason).futureValue
      DateTimeUtils.setCurrentMillisFixed(after30Min.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future.failed(new RuntimeException("error")))
      val result = banReasonsProvider.getReasonDescription(reason).futureValue
      verify(bunkerClient, times(2)).getNode(eeq(List("vertis-moderation", "autoru", "reasons", reason)))(?)
      result.value.textUserBan.value shouldBe "text"
    }

    "not retry failure before 5 minutes" in new Context {
      reset(bunkerClient)
      DateTimeUtils.setCurrentMillisFixed(startTime.getMillis)
      when(bunkerClient.getNode(?)(?)).thenReturn(Future.failed(new RuntimeException("error")))
      banReasonsProvider.getReasonDescription(reason).futureValue
      verify(bunkerClient).getNode(eeq(List("vertis-moderation", "autoru", "reasons", reason)))(?)
      DateTimeUtils.setCurrentMillisFixed(after4Min.getMillis)
      val result = banReasonsProvider.getReasonDescription(reason).futureValue
      verifyNoMoreInteractions(bunkerClient)
      result should be(empty)
    }

    "retry failure after 5 minutes" in new Context {
      DateTimeUtils.setCurrentMillisFixed(startTime.getMillis)
      when(bunkerClient.getNode(?)(?))
        .thenReturn(Future.failed(new RuntimeException("error")))
        .thenReturn(Future.successful(Some(Json.obj("text_user_ban" -> "text"))))
      banReasonsProvider.getReasonDescription(reason).futureValue
      DateTimeUtils.setCurrentMillisFixed(after5Min.getMillis)
      val result = banReasonsProvider.getReasonDescription(reason).futureValue
      verify(bunkerClient, times(2)).getNode(eeq(List("vertis-moderation", "autoru", "reasons", reason)))(?)
      result.value.textUserBan.value shouldBe "text"
    }
  }
}
