package ru.yandex.realty.rent.chat.service.processing

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.BotState
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.BotState.StateCase
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.CategoryNamespace.Category
import ru.yandex.realty.rent.chat.service.processing.context.TimeProcessingContext.NightAutoresponse
import ru.yandex.realty.rent.chat.service.processing.stages.OfferChatIdlePingProcessor
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.util.TimeUtils
import ru.yandex.realty.util.TimeUtils.instantToProtoTimestamp
import ru.yandex.vertis.chat.model.api.ApiModel.MessagePropertyType

import java.time.{Instant, LocalDateTime}
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class OfferChatIdleUserPingsSpec extends AsyncSpecBase with Matchers {

  import OfferChatBackendEventProcessorFixtures._

  trait PingFixture extends Fixture with InitialBotState {
    features.RentOfferChatIdleUserPings.setNewState(true)

    override def stage: Int = 2

    def unansweredEventTime: Instant = roomCreationTime
    override def initialRoomState: Option[RentOfferChatRoomState] = {
      super.initialRoomState.map(
        _.toBuilder
          .applySideEffect(
            _.getSessionBuilder
              .setUnansweredEventTime(instantToProtoTimestamp(unansweredEventTime))
              .setUnansweredMessageId("x")
              .applySideEffect(
                _.addMessagesToResendBuilder()
                  .applySideEffect(_.getPayloadBuilder.setValue("message to restore"))
                  .applySideEffect(
                    _.getPropertiesBuilder
                      .setType(MessagePropertyType.ACTIONS)
                      .getKeyboardBuilder
                      .addButtonsBuilder()
                      .setId("key to restore")
                      .setValue("key caption")
                  )
              )
              .applySideEffect(
                _.getBotStateToRestoreBuilder.getRentIntroBuilder
              )
          )
          .build()
      )
    }
  }

  trait TestRestoration extends PingFixture with OnMessage {
    override def eventMessageKeyboardResponse: Option[String] =
      Some(if (initialRoomState.get.getSession.getBotState.hasIdleUserPing1) "57" else "62")

    override def expectBotState: StateCase = StateCase.RENT_INTRO
    override def expectBotStateToRestore: StateCase = StateCase.RENT_INTRO
    override def expectCBMessages: Seq[CBMessage] =
      Seq(CBMessage("MIdleUserReturns"), messageWithKeyboard("message to restore", "key to restore"))
    override def checkAfterProcessing(): Unit = {
      super.checkAfterProcessing()
      savedRoomState.value.getSession.getMessagesToResendList.asScala.map(_.getPayload.getValue) shouldBe
        Seq("message to restore")
    }
  }

  trait TestMoveToFaq extends PingFixture with OnMessage with ExpectMoveToFaq {
    override def eventMessageKeyboardResponse: Option[String] = Some("58")

    override def expectBotStateToRestore: StateCase = StateCase.FAQ_CHOICE
    override def checkAfterProcessing(): Unit = {
      super.checkAfterProcessing()
      savedRoomState.value.getSession.getMessagesToResendList.asScala.map(_.getPayload.getValue) shouldBe Seq("MFaq")
    }
  }

  "ping processor" should {
    "send first ping" in new PingFixture with OnRevisit {
      override def initBotState(b: BotState.Builder): Unit = {
        // просто какое-то состояние
        b.getRentIntroBuilder
      }

      override def runBeforeProcessing(): Unit = {
        super.runBeforeProcessing()
        clock.setInstant(roomCreationTime.plus(OfferChatIdlePingProcessor.DurationUntilIdleUserPing1).plusMillis(1))
      }

      override def expectBotState: StateCase = StateCase.IDLE_USER_PING_1
      override def expectBotStateToRestore: StateCase = StateCase.RENT_INTRO
      override def expectCBMessages: Seq[CBMessage] = Seq(messageWithKeyboard("MIdleUserPing1", "57", "58", "59"))

      override def checkAfterProcessing(): Unit = {
        super.checkAfterProcessing()
        savedRoomState.value.getSession.getMessagesToResendList.asScala.map(_.getPayload.getValue) shouldBe
          Seq("message to restore")
      }
    }

    "respect night time when sending the first ping" in new PingFixture with OnRevisit {
      override def initBotState(b: BotState.Builder): Unit = {
        // просто какое-то состояние
        b.getRentIntroBuilder
      }

      override lazy val unansweredEventTime: Instant = LocalDateTime
        .of(2022, 6, 27, NightAutoresponse.NightStartHour, 0)
        .minusSeconds(1)
        .atZone(TimeUtils.MSK)
        .toInstant

      override def runBeforeProcessing(): Unit = {
        clock.setInstant(unansweredEventTime)
        super.runBeforeProcessing()
        IdleUserPingQueue.expectEnqueue()
        clock.setInstant(unansweredEventTime.plus(OfferChatIdlePingProcessor.DurationUntilIdleUserPing1).plusMillis(1))
      }

      override def expectBotState: StateCase = StateCase.RENT_INTRO
      override def expectBotStateToRestore: StateCase = StateCase.RENT_INTRO

      override def checkAfterProcessing(): Unit = {
        super.checkAfterProcessing()
        IdleUserPingQueue.enqueueAfter.value shouldBe
          LocalDateTime.of(2022, 6, 28, NightAutoresponse.NightEndHour, 0).atZone(TimeUtils.MSK).toInstant
        savedRoomState.value.getSession.getMessagesToResendList.asScala.map(_.getPayload.getValue) shouldBe
          Seq("message to restore")
      }
    }

    "send first ping in the morning" in new PingFixture with OnRevisit {
      override def initBotState(b: BotState.Builder): Unit = {
        // просто какое-то состояние
        b.getRentIntroBuilder
      }

      override lazy val unansweredEventTime: Instant = LocalDateTime
        .of(2022, 6, 27, NightAutoresponse.NightStartHour, 0)
        .minusSeconds(1)
        .atZone(TimeUtils.MSK)
        .toInstant

      override def runBeforeProcessing(): Unit = {
        clock.setInstant(unansweredEventTime)
        super.runBeforeProcessing()
        clock.setInstant(
          LocalDateTime
            .of(2022, 6, 28, NightAutoresponse.NightEndHour, 0)
            .atZone(TimeUtils.MSK)
            .toInstant
            .plusMillis(1)
        )
      }

      override def expectBotState: StateCase = StateCase.IDLE_USER_PING_1
      override def expectBotStateToRestore: StateCase = StateCase.RENT_INTRO
      override def expectCBMessages: Seq[CBMessage] = Seq(messageWithKeyboard("MIdleUserPing1", "57", "58", "59"))

      override def checkAfterProcessing(): Unit = {
        super.checkAfterProcessing()
        savedRoomState.value.getSession.getMessagesToResendList.asScala.map(_.getPayload.getValue) shouldBe
          Seq("message to restore")
      }
    }

    "send second ping" in new PingFixture with OnRevisit {
      override def initBotState(b: BotState.Builder): Unit = b.getIdleUserPing1Builder

      override def runBeforeProcessing(): Unit = {
        super.runBeforeProcessing()
        clock.setInstant(roomCreationTime.plus(OfferChatIdlePingProcessor.DurationUntilIdleUserPing2).plusMillis(1))
      }

      override def expectBotState: StateCase = StateCase.IDLE_USER_PING_2
      override def expectBotStateToRestore: StateCase = StateCase.RENT_INTRO
      override def expectCBMessages: Seq[CBMessage] = Seq(messageWithKeyboard("MIdleUserPing2", "62", "58"))
      override def checkAfterProcessing(): Unit = {
        super.checkAfterProcessing()
        savedRoomState.value.getSession.getMessagesToResendList.asScala.map(_.getPayload.getValue) shouldBe
          Seq("message to restore")
      }
    }
  }

  "first ping state" should {

    "restore old state" in new TestRestoration {
      override def initBotState(b: BotState.Builder): Unit = b.getIdleUserPing1Builder
    }

    "move to FAQ" in new TestMoveToFaq {
      override def initBotState(b: BotState.Builder): Unit = b.getIdleUserPing1Builder
    }

    "say goodbye" in new PingFixture with OnMessage with ExpectMoveToFailedBye {
      override def initBotState(b: BotState.Builder): Unit = b.getIdleUserPing1Builder

      override def eventMessageKeyboardResponse: Option[String] = Some("59")

      override def expectBotStateToRestore: StateCase = StateCase.FAILED_BYE
      override def expectNewConversationCategory: Option[Category] = Some(Category.FAILED)
      override def checkAfterProcessing(): Unit = {
        super.checkAfterProcessing()
        savedRoomState.value.getSession.getMessagesToResendList.asScala.map(_.getPayload.getValue) shouldBe
          Seq("MFailedBye")
      }
    }

  }

  "second ping state" should {

    "restore old state" in new TestRestoration {
      override def initBotState(b: BotState.Builder): Unit = b.getIdleUserPing2Builder
    }

    "move to FAQ" in new TestMoveToFaq {
      override def initBotState(b: BotState.Builder): Unit = b.getIdleUserPing1Builder
    }

  }

}
