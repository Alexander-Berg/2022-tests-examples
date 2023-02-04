package ru.yandex.realty.rent.chat.service.processing

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.chat.commands.RentOfferChatOperatorCommand
import ru.yandex.realty.chat.model.RentOfferChatCallCenterUser
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.BotState
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.BotState.StateCase
import ru.yandex.realty.rent.chat.model.offerchat.BotConfig
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.CategoryNamespace.Category
import ru.yandex.realty.tracing.Traced

import java.time.Instant
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class OfferChatBackendEventProcessorStage1Spec extends AsyncSpecBase with Matchers {

  import OfferChatBackendEventProcessorFixtures._

  trait Stage1 extends Fixture {
    override def stage: Int = 1
  }

  trait ExpectGreetingWithoutApplication {
    this: Stage1 with ProcessAndCheck =>
    final override def expectBotState: StateCase = StateCase.Q1_NO_APP
    final override def expectCBMessages: Seq[CBMessage] = Seq(
      CBMessage("MIntroduction"),
      messageWithKeyboard("MGreetNoApplication", "2", "3")
    )
  }

  trait ExpectMoveToRentIntro {
    this: Stage1 with ProcessAndCheck =>
    final override def expectBotState: StateCase = StateCase.RENT_INTRO
    final override def expectCBMessages: Seq[CBMessage] = Seq(
      CBMessage("MRentIntro1"),
      messageWithKeyboard("MRentIntro2", "17", "18")
    )
  }

  trait ExpectMoveToFaqQuestionAboutApplication extends ProcessAndCheck {
    this: Fixture =>
    override def expectBotState: StateCase = StateCase.WAIT_FOR_INPUT
    override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFaqQuestionAboutApplication"))
    override def checkAfterProcessing(): Unit = {
      super.checkAfterProcessing()
      jivositeRequests.values.size shouldBe 0
    }
  }

  trait ExpectMoveToFaqCallingManager extends ProcessAndCheck {
    this: Fixture =>
    override def expectBotState: StateCase = StateCase.WAIT_FOR_INPUT
    override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFaqCallingManager"))
    abstract override def checkAfterProcessing(): Unit = {
      super.checkAfterProcessing()
      jivositeRequests.values.size shouldBe 0
    }
  }

  trait ExpectMoveToFaq2 extends ProcessAndCheck {
    this: Fixture =>
    override def expectBotState: StateCase = StateCase.FAQ2
    def expectedFaqAnswer: String
    override def expectCBMessages: Seq[CBMessage] = Seq(messageWithKeyboard(expectedFaqAnswer, "11", "12"))
    abstract override def checkAfterProcessing(): Unit = {
      super.checkAfterProcessing()
      jivositeRequests.values.size shouldBe 0
    }
  }

  "OfferChatEventProcessor" when {

    "in stage 1 with no room state" should {

      trait InitialState extends Stage1 {
        override def initialRoomState: Option[RentOfferChatRoomState] = None
      }

      "initialize room state and send a greeting on RoomCreated WITHOUT an existing application" in
        new InitialState with OnRoomCreation with ExpectGreetingWithoutApplication {
          override def expectStartDateResponseClearance: Boolean = true
        }

      "initialize room state and send a greeting on RoomCreated WITH an existing application" in
        new InitialState with HasShowing with OnRoomCreation with ExpectGreetingWithApplication
        with ExpectActionRecord {
          override def expectStartDateResponseClearance: Boolean = true
          override def expectIntroduction: Boolean = true
        }

      "switch to direct pass if the first event is a user message" in
        new InitialState with OnMessage with ExpectMoveToDirectPass {
          override def expectAnalyticsRecordCreation: Boolean = true
          override def expectStartDateResponseClearance: Boolean = true
          override def expectedSessionStartTime: Instant = eventMessageCreated
          override def expectedSessionStartMessageId: String = eventMessageId
        }

    }

    "in stage 1 state Q1_NO_APP" should {

      trait Q1NoAppState extends Stage1 with InitialBotState {
        override def initBotState(b: BotState.Builder): Unit = b.getQ1NoAppBuilder
      }

      "present rent intro when user wants to rent" in new Q1NoAppState with OnMessage with ExpectMoveToRentIntro {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_GREET_NO_APP_RENT_FIELD_NUMBER.toString)
      }

      "switch to FAQ when user has questions" in new Q1NoAppState with OnMessage with ExpectMoveToFaq {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_GREET_NO_APP_QUESTIONS_FIELD_NUMBER.toString)
      }

      "fall back to manager on receiving free input" in new Q1NoAppState with TestFallbackToManagerOnFreeInput

      "send night autoresponse message when falling back to manager" in
        new Q1NoAppState with OnMessage with ExpectMoveToDirectPass {

          override def runBeforeProcessing(): Unit = {
            clock.setInstant(Instant.ofEpochSecond(22 * 60 * 60))
            super.runBeforeProcessing()
          }

          override def expectCBMessages: Seq[CBMessage] = Seq(
            CBMessage("MFallbackToManager"),
            CBMessage("MNightAutoresponse 01:00 09:00")
          )

        }

      "restart from beginning on 'want to rent' answer if showing exists" in
        new Q1NoAppState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_GREET_NO_APP_RENT_FIELD_NUMBER.toString)
        }

      "switch to FAQ when user has questions even when a showing exists" in
        new Q1NoAppState with HasShowing with OnMessage with ExpectMoveToFaq with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_GREET_NO_APP_QUESTIONS_FIELD_NUMBER.toString)
        }

    }

    "in stage 1 state Q1_WITH_APP" should {

      trait Q1WithAppState extends Stage1 with HasShowing with InitialBotState {
        override def initBotState(b: BotState.Builder): Unit = b.getQ1WithAppBuilder
      }

      "wait for input before calling the manager when user chooses a question about their application" in
        new Q1WithAppState with OnMessage with ExpectMoveToFaqQuestionAboutApplication {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_GREET_WITH_APP_QUESTION_FIELD_NUMBER.toString)
        }

      "prompt for the question and wait for input when user says they have another question" in
        new Q1WithAppState with OnMessage with ExpectMoveToFaqCallingManager {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_GREET_WITH_APP_OTHER_FIELD_NUMBER.toString)
        }

      "fall back to manager on receiving free input" in new Q1WithAppState with TestFallbackToManagerOnFreeInput

    }

    "in stage 1 state FAQ_CHOICE" should {

      trait FaqChoiceState extends Stage1 with InitialBotState {
        override def initBotState(b: BotState.Builder): Unit = b.getFaqChoiceBuilder
      }

      "wait for input before calling the manager when user chooses a question about their application" in
        new FaqChoiceState with OnMessage with ExpectMoveToFaqQuestionAboutApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_FAQ_QUESTION_ABOUT_APPLICATION_FIELD_NUMBER.toString)
          override def expectNewConversationCategory: Option[Category] = Some(Category.CLIENT)
        }

      "wait for input before calling the manager when asked" in
        new FaqChoiceState with OnMessage with ExpectMoveToFaqCallingManager {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_FAQ_SPEAK_TO_MANAGER_FIELD_NUMBER.toString)
        }

      "answer the chosen question when it is enabled" in new FaqChoiceState with OnMessage with ExpectMoveToFaq2 {
        override def eventMessageKeyboardResponse: Option[String] = Some("Id")
        override def expectedFaqAnswer: String = "Answer"
      }
      "answer the chosen question even when it is disabled" in new FaqChoiceState with OnMessage with ExpectMoveToFaq2 {
        override def eventMessageKeyboardResponse: Option[String] = Some("DisabledId")
        override def expectedFaqAnswer: String = "DisabledAnswer"
      }
      "fall back to manager when question selection is unrecognized" in
        new FaqChoiceState with OnMessage with ExpectMoveToDirectPass {
          override def eventMessageKeyboardResponse: Option[String] = Some("UnknownId")
          override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFallbackToManager"))
        }

      "fall back to manager on receiving free input" in new FaqChoiceState with TestFallbackToManagerOnFreeInput

    }

    "in stage 1 state FAQ2" should {

      "reset category and fall back to manager when user wants to continue after a FAILED status was set" in
        new Stage1 with Faq2State with InitialCategory with OnMessage with ExpectMoveToDirectPass {
          override def currentConversationCategory: Category = Category.FAILED
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_FAQ_WANT_TO_RENT_FIELD_NUMBER.toString)

          override def expectNewConversationCategory: Option[Category] = Some(Category.NEW)
          override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFallbackToManager"))
        }

      "set category CLIENT and offer FAQ when user wants to continue after a successful status was set" in
        new Stage1 with Faq2State with InitialCategory with OnMessage with ExpectGreetingWithApplication
        with ExpectActionRecord {
          override def currentConversationCategory: Category = Category.ONLINE_SHOWING
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_FAQ_WANT_TO_RENT_FIELD_NUMBER.toString)
        }

      "move to main flow if requested" in new Stage1 with Faq2State with OnMessage with ExpectMoveToRentIntro {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_FAQ_WANT_TO_RENT_FIELD_NUMBER.toString)
      }

      "show questions again if requested" in new Stage1 with Faq2State with OnMessage with ExpectMoveToFaq {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_FAQ_MORE_QUESTIONS_FIELD_NUMBER.toString)
      }

      "fall back to manager on receiving free input" in new Stage1 with Faq2State with TestFallbackToManagerOnFreeInput

    }

    "in stage 1 state RENT_INTRO" should {

      trait RentIntroState extends Stage1 with InitialBotState {
        override def initBotState(b: BotState.Builder): Unit = b.getRentIntroBuilder
      }

      "move to DIRECT_PASS if user agrees" in new RentIntroState with OnMessage with ExpectMoveToDirectPass {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_INTRO_CONTINUE_FIELD_NUMBER.toString)
        override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFallbackToManager"))
      }

      "switch to FAQ if user has questions" in new RentIntroState with OnMessage with ExpectMoveToFaq {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_INTRO_QUESTIONS_FIELD_NUMBER.toString)
      }

      "fall back to manager on receiving free input" in new RentIntroState with TestFallbackToManagerOnFreeInput

      "restart from beginning on 'continue' answer if showing exists" in
        new RentIntroState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_INTRO_CONTINUE_FIELD_NUMBER.toString)
        }

      "switch to FAQ if user has questions even when a showing exists" in
        new RentIntroState with HasShowing with OnMessage with ExpectMoveToFaq {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_INTRO_QUESTIONS_FIELD_NUMBER.toString)
        }

    }

    "in stage 1 state WAIT_FOR_INPUT" should {

      trait WaitForInputState extends Stage1 with InitialBotState {
        override def initBotState(b: BotState.Builder): Unit = b.getWaitForInputBuilder
      }

      "move to DIRECT_PASS on receiving free input" in new WaitForInputState with OnMessage with ExpectMoveToDirectPass

    }

    "in stage 1 state DIRECT_PASS" should {

      "pass user message through" in new Stage1 with DirectPassState with OnDirectPassMessage {
        override def expectBotState: StateCase = StateCase.DIRECT_PASS
        override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET

        override def checkFinalRoomState(): Unit = {
          super.checkFinalRoomState()
          savedRoomState.value.getSession.getLastMessageSentToOperator shouldBe eventMessageId
        }

        override def checkAfterProcessing(): Unit = {
          super.checkAfterProcessing()
          jivositeRequests.values.size should be > 0
        }
      }

      "mark chat as closed when operator commands" in new Stage1 with DirectPassState with OnDirectPassMessage {
        override def eventMessageAuthor: String = RentOfferChatCallCenterUser.toPlain
        override def eventMessageText: String = RentOfferChatOperatorCommand.Close.text

        override def expectBotState: StateCase = StateCase.DIRECT_PASS
        override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
        override def expectChatClosed: Boolean = true

        override def checkAfterProcessing(): Unit = {
          super.checkAfterProcessing()
          jivositeRequests.values.size shouldBe 0
        }
      }

      "unmark chat as closed on user message" in new Stage1 with DirectPassState with Closed with OnDirectPassMessage {
        override def expectBotState: StateCase = StateCase.DIRECT_PASS
        override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
        override def expectChatClosed: Boolean = false
      }

      "unmark chat as closed on operator message" in
        new Stage1 with DirectPassState with Closed with OnDirectPassMessage {
          override def eventMessageAuthor: String = RentOfferChatCallCenterUser.toPlain

          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
          override def expectChatClosed: Boolean = false
        }

    }

  }

}
