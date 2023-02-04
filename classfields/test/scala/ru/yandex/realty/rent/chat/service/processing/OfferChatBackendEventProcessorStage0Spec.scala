package ru.yandex.realty.rent.chat.service.processing

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.BotState
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.BotState.StateCase
import ru.yandex.realty.rent.chat.model.offerchat.BotConfig
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.CategoryNamespace.Category
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils

import java.time.Instant
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class OfferChatBackendEventProcessorStage0Spec extends AsyncSpecBase with Matchers {

  import OfferChatBackendEventProcessorFixtures._

  trait Stage0ProcessAndCheck extends Fixture with ProcessAndCheck {
    override def stage: Int = 0
    final override def expectBotState: StateCase = StateCase.STATE_NOT_SET
  }

  "OfferChatEventProcessor" when {

    "in stage 0 with no room state" should {

      "initialize room state on RoomCreated" in new Stage0ProcessAndCheck with OnRoomCreation {
        override def initialRoomState: Option[RentOfferChatRoomState] = None
      }

      "pass context and user message through on first message" in new Stage0ProcessAndCheck with OnMessage {
        override def initialRoomState: Option[RentOfferChatRoomState] = None

        override def expectedSessionStartTime: Instant = eventMessageCreated
        override def expectedSessionStartMessageId: String = eventMessageId

        override def checkFinalRoomState(): Unit = {
          super.checkFinalRoomState()
          savedRoomState.value.getSession.getLastMessageSentToOperator shouldBe eventMessageId
        }

        override def checkAfterProcessing(): Unit = {
          super.checkAfterProcessing()
          jivositeRequests.values.size shouldBe 2
          jivositeRequests.values(0).message.get.text.get should startWith("Адрес квартиры: address with flat")
          jivositeRequests.values(1).message.get.text.get shouldBe eventMessageText
        }
      }

      "pass only user message through on second message" in new Stage0ProcessAndCheck with OnMessage {
        override def initialRoomState: Option[RentOfferChatRoomState] = Some(
          RentOfferChatRoomState
            .newBuilder()
            .applySideEffect(
              _.getSessionBuilder
                .setLastMessageSentToOperator("x")
                .getLimitsBuilder
                .setFrom(TimeUtils.instantToProtoTimestamp(roomCreationTime))
                .setTo(TimeUtils.instantToProtoTimestamp(roomCreationTime))
            )
            .build()
        )

        override def runBeforeProcessing(): Unit = {
          super.runBeforeProcessing()
          // такой запрос сделает DirectPassProcessor;
          // притворяемся, что сообщение "x" было сразу перед сообщением из события
          (historyLoader
            .loadHistoryFrom(_: String, _: Option[String], _: Boolean, _: Instant)(_: Traced))
            .expects(roomId, Some("x"), true, eventMessageCreated, *)
            .atLeastOnce()
            .returns(Future.successful(Seq(message)))
        }

        override def checkFinalRoomState(): Unit = {
          super.checkFinalRoomState()
          savedRoomState.value.getSession.getLastMessageSentToOperator shouldBe eventMessageId
        }

        override def checkAfterProcessing(): Unit = {
          super.checkAfterProcessing()
          jivositeRequests.values.size shouldBe 1
          jivositeRequests.value.message.get.text.get shouldBe eventMessageText
        }
      }

    }

  }

}
