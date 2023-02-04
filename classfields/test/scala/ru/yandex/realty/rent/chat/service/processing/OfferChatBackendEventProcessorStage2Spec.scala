package ru.yandex.realty.rent.chat.service.processing

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.{AsyncSpecBase, CommonConstants}
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.BotState.StateCase
import ru.yandex.realty.rent.chat.model.offerchat.BotConfig
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.{ApplicationData, BotState}
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.CategoryNamespace.Category
import ru.yandex.realty.rent.chat.service.processing.context.TimeProcessingContext.NightAutoresponse
import ru.yandex.realty.rent.chat.service.processing.stages.OfferChatIdlePingProcessor
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.util.TimeUtils

import java.time.{Instant, LocalDateTime}
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class OfferChatBackendEventProcessorStage2Spec extends AsyncSpecBase with Matchers {

  import OfferChatBackendEventProcessorFixtures._

  trait Stage2 extends Fixture {
    override def stage: Int = 2

    def maxTenants: Option[Int] = None
    def allowPets: Boolean = false
    (flatQuestionnaireClient
      .getFlatQuestionnaireByOwnerRequestId(_: String)(_: Traced))
      .expects(ownerRequestId, *)
      .anyNumberOfTimes()
      .returns(
        Future.successful(
          Some(
            FlatQuestionnaire
              .newBuilder()
              .applySideEffects[Int](maxTenants, _.getTenantRequirementsBuilder.setMaxTenantCount(_))
              .applySideEffect(_.getTenantRequirementsBuilder.setHasWithPetsRequirement(allowPets))
              .build()
          )
        )
      )
  }

  trait ExpectMoveToFailureBranch {
    this: Stage2 with ProcessAndCheck =>

    final override def expectBotState: StateCase = StateCase.FAILED_PROMPT

    final override def expectNewConversationCategory: Option[Category] = Some(Category.FAILED)

    def expectMessagesBeforeFailedPrompt: Seq[CBMessage] = Seq.empty

    final override def expectCBMessages: Seq[CBMessage] =
      expectMessagesBeforeFailedPrompt :+ messageWithKeyboard("MFailedPrompt", "21", "22")
  }

  trait ExpectMoveToLongTermOnly extends ExpectIdleUserPingEnqueued {
    this: Stage2 with OnMessage =>
    final override def expectBotState: StateCase = StateCase.LONG_TERM_ONLY
    def expectReturningMessage: Boolean = false
    final override def expectCBMessages: Seq[CBMessage] =
      (if (expectReturningMessage) Seq(CBMessage("MReturningToMainFlow")) else Seq.empty) :+
        messageWithKeyboard("MLongTermOnly", "25", "26")
  }

  trait ExpectMoveToTenants extends ExpectIdleUserPingEnqueued {
    this: Stage2 with OnMessage =>
    final override def expectBotState: StateCase = StateCase.TENANTS
    def expectReturningMessage: Boolean = false
    final override def expectCBMessages: Seq[CBMessage] =
      (if (expectReturningMessage) Seq(CBMessage("MReturningToMainFlow")) else Seq.empty) :+
        messageWithKeyboard("MTenants", "49", "50", "51", "52", "53")
  }

  trait ExpectMoveToPets extends ExpectIdleUserPingEnqueued {
    this: Stage2 with OnMessage =>
    final override def expectBotState: StateCase = StateCase.PETS
    final override def expectCBMessages: Seq[CBMessage] = Seq(messageWithKeyboard("MPets", "31", "32"))
  }

  trait ExpectMoveToPetsWhich extends ExpectIdleUserPingEnqueued {
    this: Stage2 with OnMessage =>
    final override def expectBotState: StateCase = StateCase.PETS_WHICH
    final override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MPetsWhich"))
  }

  trait ExpectMoveToStartDatePrompt extends ExpectIdleUserPingEnqueued {
    this: Stage2 with OnMessage =>
    final override def expectBotState: StateCase = StateCase.START_DATE_PROMPT
    final override def expectCBMessages: Seq[CBMessage] =
      Seq(messageWithKeyboard("MStartDatePromptUnlimited", "0", "1"))
  }

  trait ExpectMoveToCitizenship extends ExpectIdleUserPingEnqueued {
    this: Stage2 with OnMessage =>
    final override def expectBotState: StateCase = StateCase.CITIZENSHIP
    final override def expectCBMessages: Seq[CBMessage] =
      Seq(messageWithKeyboard("MCitizenship", "38", "39"))
  }

  trait ExpectMoveToShowingType extends ExpectIdleUserPingEnqueued {
    this: Stage2 with OnMessage =>
    final override def expectBotState: StateCase = StateCase.SHOWING_TYPE
    final override def expectCBMessages: Seq[CBMessage] =
      Seq(messageWithKeyboard("MShowingType", "42", "43", "44"))
  }

  trait ExpectMoveToShowingTypeV2 extends ExpectIdleUserPingEnqueued {
    this: Stage2 with OnMessage =>
    final override def expectBotState: StateCase = StateCase.SHOWING_TYPE
    final override def expectCBMessages: Seq[CBMessage] =
      Seq(CBMessage("MShowingType2 1"), messageWithKeyboard("MShowingType2 2", "42", "44"))
  }

  "OfferChatEventProcessor" when {

    "in stage 2 state RENT_INTRO" should {

      trait RentIntroState extends Stage2 with InitialBotState {
        override def initBotState(b: BotState.Builder): Unit = b.getRentIntroBuilder
      }

      "init AppData and ask about rent term if user agrees" in
        new RentIntroState with OnMessage with ExpectMoveToLongTermOnly {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_INTRO_CONTINUE_FIELD_NUMBER.toString)

          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.hasApplicationData shouldBe true
          }
        }

      "keep AppData if it was already filled when asking the next question" in
        new RentIntroState with InitAppData with OnMessage with ExpectMoveToLongTermOnly {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            // просто какой-то не начальный вопрос
            adb.getCitizenshipBuilder
          }

          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_INTRO_CONTINUE_FIELD_NUMBER.toString)

          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.hasApplicationData shouldBe true
            savedRoomState.value.getSession.getApplicationData.hasCitizenship shouldBe true
          }
        }

      "switch to FAQ if user has more questions" in new RentIntroState with OnMessage with ExpectMoveToFaq {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_INTRO_QUESTIONS_FIELD_NUMBER.toString)
      }

      "fall back to manager on receiving free input" in new RentIntroState with TestFallbackToManagerOnFreeInput

      "restart from beginning on 'continue' answer if showing exists" in
        new RentIntroState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_INTRO_CONTINUE_FIELD_NUMBER.toString)
        }

      "switch to FAQ if user has more questions even when there is a showing" in
        new RentIntroState with HasShowing with OnMessage with ExpectMoveToFaq {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_INTRO_QUESTIONS_FIELD_NUMBER.toString)
        }

      "move to direct pass on free input if showing exists" in
        new RentIntroState with TestFallbackToManagerOnFreeInputWithShowing

    }

    "in stage 2 state LONG_TERM_ONLY" should {

      trait LTOState extends Stage2 with InitialBotState with InitAppData {
        override def initBotState(b: BotState.Builder): Unit = b.getLongTermOnlyBuilder
        override def initAppData(adb: ApplicationData.Builder): Unit = {}
      }
      trait AnsweredAccept extends OnMessage {
        this: Fixture =>
        final override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_LONG_TERM_ONLY_ACCEPT_FIELD_NUMBER.toString)
      }

      "record answer and move to next section if user agrees" in
        new LTOState with OnMessage with AnsweredAccept with ExpectMoveToTenants {
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasLongTermOnly shouldBe true
          }
        }
      "skip answered questions" in
        new LTOState with OnMessage with AnsweredAccept with ExpectMoveToPets {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.getTenantsBuilder
          }
        }

      "move to failed branch if user declines" in new LTOState with OnMessage with ExpectMoveToFailureBranch {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_LONG_TERM_ONLY_NO_FIELD_NUMBER.toString)
        override def expectMessagesBeforeFailedPrompt: Seq[CBMessage] =
          Seq(
            CBMessage(
              s"MLongTermOnlyUnfit ${CommonConstants.REALTY_BASE_URL}/daily_rent_search_url rent_offer_chat_term"
            )
          )
      }

      "fall back to manager on receiving free input" in new LTOState with TestFallbackToManagerOnFreeInput

      "restart from beginning on 'continue' answer if showing exists" in
        new LTOState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_LONG_TERM_ONLY_ACCEPT_FIELD_NUMBER.toString)
        }

      "restart from beginning on 'decline' answer if showing exists" in
        new LTOState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_LONG_TERM_ONLY_NO_FIELD_NUMBER.toString)
        }

      "move to direct pass on free input if showing exists" in
        new LTOState with TestFallbackToManagerOnFreeInputWithShowing

    }

    "in stage 2 state TENANTS" should {

      trait TenantsState extends Stage2 with InitialBotState with InitAppData {
        override def initBotState(b: BotState.Builder): Unit = b.getTenantsBuilder
        override def initAppData(adb: ApplicationData.Builder): Unit = {
          adb.getLongTermOnlyBuilder
        }
      }
      trait AnsweredWithValidNumber extends Stage2 with OnMessage {
        final override def maxTenants: Option[Int] = Some(1)
        override def eventMessageText: String = "1"
      }

      "record answer and move to next section if user enters a valid number" in
        new TenantsState with AnsweredWithValidNumber with ExpectMoveToPets {
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasTenants shouldBe true
          }
        }
      "record answer and move to next section if user enters a number that we can't verify" in
        new TenantsState with OnMessage with ExpectMoveToPets {
          override def maxTenants: Option[Int] = None
          override def eventMessageText: String = "1000"

          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasTenants shouldBe true
          }
        }
      "move to failed branch if user enters an invalid number" in
        new TenantsState with OnMessage with ExpectMoveToFailureBranch {
          override def maxTenants: Option[Int] = Some(1)
          override def eventMessageText: String = "1000"
          override def expectMessagesBeforeFailedPrompt: Seq[CBMessage] =
            Seq(
              CBMessage(
                s"MTenantsUnfit ${CommonConstants.REALTY_BASE_URL}/yandex_rent_search_url rent_offer_chat_tenants"
              )
            )
        }

      "record answer and move to next section if user replies with a valid preset number" in
        new TenantsState with OnMessage with ExpectMoveToPets {
          override def maxTenants: Option[Int] = Some(1)
          override def eventMessageKeyboardResponse: Option[String] = Some("49")

          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasTenants shouldBe true
          }
        }
      "record answer and move to next section if user replies with a preset number that we can't verify" in
        new TenantsState with OnMessage with ExpectMoveToPets {
          override def maxTenants: Option[Int] = None
          override def eventMessageKeyboardResponse: Option[String] = Some("53")

          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasTenants shouldBe true
          }
        }
      "move to failed branch if user replies with an invalid preset number" in
        new TenantsState with OnMessage with ExpectMoveToFailureBranch {
          override def maxTenants: Option[Int] = Some(1)
          override def eventMessageKeyboardResponse: Option[String] = Some("50")
          override def expectMessagesBeforeFailedPrompt: Seq[CBMessage] =
            Seq(
              CBMessage(
                s"MTenantsUnfit ${CommonConstants.REALTY_BASE_URL}/yandex_rent_search_url rent_offer_chat_tenants"
              )
            )
        }
      "fall back to manager if user replies with an unrecognized preset" in
        new TenantsState with OnMessage with ExpectMoveToDirectPass {
          override def maxTenants: Option[Int] = None
          override def eventMessageKeyboardResponse: Option[String] = Some("0")
          override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFallbackToManager"))
        }

      "skip answered questions" in
        new TenantsState with AnsweredWithValidNumber with ExpectMoveToStartDatePrompt {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            super.initAppData(adb)
            adb.getPetsBuilder
          }
        }
      "return to unanswered questions" in
        new TenantsState with AnsweredWithValidNumber with ExpectMoveToLongTermOnly {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.clearLongTermOnly()
          }
        }

      "fall back to manager on receiving free input" in new TenantsState with TestFallbackToManagerOnFreeInput

      "restart from beginning on a number response if showing exists" in
        new TenantsState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageText: String = "1"
        }
      "restart from beginning on a preset response if showing exists" in
        new TenantsState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] = Some("49")
        }
      "move to direct pass on an unparseable response if showing exists" in
        new TenantsState with TestFallbackToManagerOnFreeInputWithShowing

    }

    "in stage 2 state PETS" should {

      trait PetsState extends Stage2 with InitialBotState with InitAppData {
        override def initBotState(b: BotState.Builder): Unit = b.getPetsBuilder
        override def initAppData(adb: ApplicationData.Builder): Unit = {
          adb.getLongTermOnlyBuilder
          adb.getTenantsBuilder
        }
      }
      trait AnsweredNo extends Stage2 with OnMessage {
        override def eventMessageKeyboardResponse: Option[String] = Some(BotConfig.A_PETS_NO_FIELD_NUMBER.toString)
      }

      "fall back to manager on receiving free input" in new PetsState with TestFallbackToManagerOnFreeInput

      "record answer and move to next section if user does not have pets" in
        new PetsState with AnsweredNo with ExpectMoveToStartDatePrompt {
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasPets shouldBe true
          }
        }

      "skip answered questions" in
        new PetsState with AnsweredNo with ExpectMoveToCitizenship {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.getLongTermOnlyBuilder
            adb.getTenantsBuilder
            adb.getStartDateBuilder
          }
        }
      "return to unanswered questions" in
        new PetsState with AnsweredNo with ExpectMoveToLongTermOnly {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.clearLongTermOnly()
          }
        }

      "move to failed branch if user has a pet but owner does not allow pets" in
        new PetsState with OnMessage with ExpectMoveToFailureBranch {
          override def allowPets: Boolean = false
          override def eventMessageKeyboardResponse: Option[String] = Some(BotConfig.A_PETS_YES_FIELD_NUMBER.toString)
          override def expectMessagesBeforeFailedPrompt: Seq[CBMessage] =
            Seq(
              CBMessage(
                s"MPetsNotAllowed ${CommonConstants.REALTY_BASE_URL}/yandex_rent_with_pets_search_url rent_offer_chat_pets"
              )
            )
        }

      "ask for pet description if user has a pet and owner allows pets" in
        new PetsState with OnMessage with ExpectMoveToPetsWhich {
          override def allowPets: Boolean = true
          override def eventMessageKeyboardResponse: Option[String] = Some(BotConfig.A_PETS_YES_FIELD_NUMBER.toString)
        }

      "move to direct pass on free input if showing exists" in
        new PetsState with TestFallbackToManagerOnFreeInputWithShowing

    }

    "in stage 2 state PETS_WHICH" should {

      trait PetsWhichState extends Stage2 with InitialBotState with InitAppData {
        override def initBotState(b: BotState.Builder): Unit = b.getPetsWhichBuilder
        override def initAppData(adb: ApplicationData.Builder): Unit = {
          adb.getLongTermOnlyBuilder
          adb.getTenantsBuilder
        }
      }

      "record answer and move to next section after user input" in
        new PetsWhichState with OnMessage with ExpectMoveToStartDatePrompt {
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasPets shouldBe true
          }
        }

      "skip answered questions" in
        new PetsWhichState with OnMessage with ExpectMoveToCitizenship {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.getLongTermOnlyBuilder
            adb.getTenantsBuilder
            adb.getStartDateBuilder
          }
        }
      "return to unanswered questions" in
        new PetsWhichState with OnMessage with ExpectMoveToLongTermOnly {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.clearLongTermOnly()
          }
        }

      "move to direct pass on free input if showing exists" in
        new PetsWhichState with TestFallbackToManagerOnFreeInputWithShowing

    }

    "in stage 2 state START_DATE_PROMPT" should {

      trait StartDatePromptState extends Stage2 with InitialBotState with InitAppData {
        override def initBotState(b: BotState.Builder): Unit = b.getStartDatePromptBuilder
        override def initAppData(adb: ApplicationData.Builder): Unit = {
          adb.getLongTermOnlyBuilder
          adb.getTenantsBuilder
          adb.getPetsBuilder
        }
      }

      "record answer and move to next section after user input" in
        new StartDatePromptState with OnMessage with ExpectMoveToCitizenship {
          override def expectStartDateResponseSetting: Boolean = true

          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasStartDate shouldBe true
          }
        }

      "skip answered questions" in
        new StartDatePromptState with OnMessage with ExpectMoveToShowingType {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.getLongTermOnlyBuilder
            adb.getTenantsBuilder
            adb.getPetsBuilder
            adb.getCitizenshipBuilder
          }
          override def expectStartDateResponseSetting: Boolean = true
        }
      "return to unanswered questions" in
        new StartDatePromptState with OnMessage with ExpectMoveToLongTermOnly {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.clearLongTermOnly()
          }
          override def expectStartDateResponseSetting: Boolean = true
        }

      "move to direct pass on free input if showing exists" in
        new StartDatePromptState with TestFallbackToManagerOnFreeInputWithShowing

    }

    "in stage 2 state CITIZENSHIP" should {

      trait CitizenshipState extends Stage2 with InitialBotState with InitAppData {
        override def initBotState(b: BotState.Builder): Unit = b.getCitizenshipBuilder
        override def initAppData(adb: ApplicationData.Builder): Unit = {
          adb.getLongTermOnlyBuilder
          adb.getTenantsBuilder
          adb.getPetsBuilder
          adb.getStartDateBuilder
        }
      }
      trait AnsweredYes extends Stage2 with OnMessage {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_CITIZENSHIP_YES_FIELD_NUMBER.toString)
      }

      "record answer and move to next section on positive answer" in
        new CitizenshipState with AnsweredYes with ExpectMoveToShowingType {
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasCitizenship shouldBe true
          }
        }

      "record answer and move to next section on positive answer with showing type v2 enabled" in
        new CitizenshipState with AnsweredYes with ExpectMoveToShowingTypeV2 {
          override def runBeforeProcessing(): Unit = {
            features.RentOfferChatShowingTypeV2.setNewState(true)
            super.runBeforeProcessing()
          }

          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasCitizenship shouldBe true
          }
        }

      "skip answered questions" in
        new CitizenshipState with AnsweredYes with ExpectMoveToDirectPass {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.getLongTermOnlyBuilder
            adb.getTenantsBuilder
            adb.getPetsBuilder
            adb.getStartDateBuilder
            adb.getShowingTypeBuilder
          }
          override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFallbackToManager"))
        }
      "return to unanswered questions" in
        new CitizenshipState with AnsweredYes with ExpectMoveToLongTermOnly {
          override def initAppData(adb: ApplicationData.Builder): Unit = {
            adb.clearLongTermOnly()
          }
        }

      "move to failure branch on negative answer" in
        new CitizenshipState with OnMessage with ExpectMoveToFailureBranch {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_CITIZENSHIP_NO_FIELD_NUMBER.toString)
          override def expectMessagesBeforeFailedPrompt: Seq[CBMessage] = Seq(CBMessage("MCitizenshipUnfit"))
        }

      "fall back to manager on receiving free input" in new CitizenshipState with TestFallbackToManagerOnFreeInput

      "restart from beginning on positive answer if showing exists" in
        new CitizenshipState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_CITIZENSHIP_YES_FIELD_NUMBER.toString)
        }

      "restart from beginning on negative answer if showing exists" in
        new CitizenshipState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_CITIZENSHIP_NO_FIELD_NUMBER.toString)
        }

      "move to direct pass on free input if showing exists" in
        new CitizenshipState with TestFallbackToManagerOnFreeInputWithShowing

    }

    "in stage 2 state SHOWING_TYPE" should {

      trait ShowingTypeState extends Stage2 with InitialBotState {
        override def initBotState(b: BotState.Builder): Unit = b.getShowingTypeBuilder
      }

      "record answer and move to DIRECT_PASS on WITHOUT answer" in
        new ShowingTypeState with OnMessage with ExpectMoveToDirectPass {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_SHOWING_TYPE_WITHOUT_FIELD_NUMBER.toString)
          override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFallbackToManager"))
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasShowingType shouldBe true
          }
        }
      "record answer and move to DIRECT_PASS on ONLINE answer" in
        new ShowingTypeState with OnMessage with ExpectMoveToDirectPass {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_SHOWING_TYPE_ONLINE_FIELD_NUMBER.toString)
          override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFallbackToManager"))
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasShowingType shouldBe true
          }
        }
      "record answer and move to DIRECT_PASS on OFFLINE answer" in
        new ShowingTypeState with OnMessage with ExpectMoveToDirectPass {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_SHOWING_TYPE_OFFLINE_FIELD_NUMBER.toString)
          override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFallbackToManager"))
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            savedRoomState.value.getSession.getApplicationData.hasShowingType shouldBe true
          }
        }

      "fall back to manager on receiving free input" in new ShowingTypeState with TestFallbackToManagerOnFreeInput

      "restart from beginning on WITHOUT answer if showing exists" in
        new ShowingTypeState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_SHOWING_TYPE_WITHOUT_FIELD_NUMBER.toString)
        }
      "restart from beginning on ONLINE answer if showing exists" in
        new ShowingTypeState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_SHOWING_TYPE_ONLINE_FIELD_NUMBER.toString)
        }
      "restart from beginning on OFFLINE answer if showing exists" in
        new ShowingTypeState with HasShowing with OnMessage with ExpectGreetingWithApplication with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_SHOWING_TYPE_OFFLINE_FIELD_NUMBER.toString)
        }

      "move to direct pass on free input if showing exists" in
        new ShowingTypeState with TestFallbackToManagerOnFreeInputWithShowing

    }

    "in stage 2 state FAILED_PROMPT" should {

      trait FailedPromptState extends Stage2 with InitialBotState {
        override def initBotState(b: BotState.Builder): Unit = b.getFailedPromptBuilder
      }

      "switch to FAQ if user replies positively" in new FailedPromptState with OnMessage with ExpectMoveToFaq {
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_FAILED_PROMPT_YES_FIELD_NUMBER.toString)
      }

      "move to FAILED_BYE if user replies negatively" in
        new FailedPromptState with OnMessage with ExpectMoveToFailedBye {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_FAILED_PROMPT_NO_FIELD_NUMBER.toString)
        }

      "fall back to manager on receiving free input" in new FailedPromptState with TestFallbackToManagerOnFreeInput

      "restart from beginning on positive answer if showing exists" in
        new FailedPromptState with HasShowing with OnMessage with ExpectGreetingWithApplication
        with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_FAILED_PROMPT_YES_FIELD_NUMBER.toString)
        }
      "restart from beginning on negative answer if showing exists" in
        new FailedPromptState with HasShowing with OnMessage with ExpectGreetingWithApplication
        with ExpectActionRecord {
          override def eventMessageKeyboardResponse: Option[String] =
            Some(BotConfig.A_FAILED_PROMPT_NO_FIELD_NUMBER.toString)
        }

      "move to direct pass on free input if showing exists" in
        new FailedPromptState with TestFallbackToManagerOnFreeInputWithShowing

    }

    "in stage 2 state FAILED_BYE" should {

      trait FailedByeState extends Stage2 with InitialBotState {
        override def initBotState(b: BotState.Builder): Unit = b.getFailedByeBuilder
      }

      "fall back to manager on receiving free input" in new FailedByeState with TestFallbackToManagerOnFreeInput

    }

    "in stage 2 state DIRECT_PASS" should {

      trait UserMessageStreak extends Fixture {
        def initialStreakTime: Instant = Instant.ofEpochMilli(12345)
        def initialStreakPinged: Boolean = false
        abstract override def initialRoomState: Option[RentOfferChatRoomState] = super.initialRoomState.map { rs =>
          rs.toBuilder
            .applySideEffect(
              _.getSessionBuilder.getCurrentUserMessageStreakBuilder
                .setStartTime(TimeUtils.instantToProtoTimestamp(initialStreakTime))
                .setStartMessageId("y")
                .setIdleOperatorPingSent(initialStreakPinged)
            )
            .build()
        }
      }

      trait IdleOperatorPingsEnabled extends Fixture {
        features.RentOfferChatIdleManagerPings.setNewState(true)
      }

      trait ExpectStreakInState extends ProcessAndCheck {
        this: Fixture =>
        def expectStreakStart: Instant = Instant.ofEpochMilli(12345)
        def expectStreakPinged: Boolean = false
        abstract override def checkFinalRoomState(): Unit = {
          super.checkFinalRoomState()
          savedRoomState.value.getSession.getCurrentUserMessageStreak.getStartTime shouldBe
            TimeUtils.instantToProtoTimestamp(expectStreakStart)
          savedRoomState.value.getSession.getCurrentUserMessageStreak.getIdleOperatorPingSent shouldBe expectStreakPinged
        }
      }

      "mark the start of a user message streak and enqueue a ping" in
        new Stage2 with DirectPassState with IdleOperatorPingsEnabled with OnDirectPassMessage {
          override def emulateMoreUnprocessedMessages: Boolean = false

          override def runBeforeProcessing(): Unit = {
            super.runBeforeProcessing()
            IdleOperatorPingQueue.expectEnqueue()
          }

          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET

          override def checkFinalRoomState(): Unit = {
            super.checkFinalRoomState()
            savedRoomState.value.getSession.getCurrentUserMessageStreak.getStartTime shouldBe
              TimeUtils.instantToProtoTimestamp(eventMessageCreated)
            savedRoomState.value.getSession.getCurrentUserMessageStreak.getIdleOperatorPingSent shouldBe false
          }

          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            IdleOperatorPingQueue.enqueueAfter.value shouldBe
              eventMessageCreated.plus(OfferChatIdlePingProcessor.DurationUntilIdleOperatorPing)
          }
        }

      "not overwrite the start of a user message streak" in
        new Stage2 with DirectPassState with UserMessageStreak with OnDirectPassMessage with ExpectStreakInState {
          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
        }

      "clear user message streak on operator message" in
        new Stage2 with DirectPassState with UserMessageStreak with OnDirectPassMessage with OnOperatorMessage {
          override def expectBotState: StateCase = StateCase.DIRECT_PASS

          override def checkFinalRoomState(): Unit = {
            super.checkFinalRoomState()
            savedRoomState.value.getSession.hasCurrentUserMessageStreak shouldBe false
          }
        }

      "not clear user message streak on bot message" in
        new Stage2 with DirectPassState with UserMessageStreak with OnDirectPassMessage with OnBotMessage
        with ExpectStreakInState {
          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
        }

      "not send idle operator pings when there are unprocessed messages" in
        new Stage2 with DirectPassState with UserMessageStreak with IdleOperatorPingsEnabled with OnRevisit {
          override def emulateMoreUnprocessedMessages: Boolean = true

          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
          override def expectCBMessages: Seq[CBMessage] = Seq.empty
        }

      "not send idle operator pings when the current user streak has already been pinged" in
        new Stage2 with DirectPassState with UserMessageStreak with IdleOperatorPingsEnabled with OnRevisit {
          override def initialStreakPinged: Boolean = true
          override def emulateMoreUnprocessedMessages: Boolean = false

          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
          override def expectCBMessages: Seq[CBMessage] = Seq.empty
        }

      "not send idle operator pings when the chat is closed by operator" in
        new Stage2 with DirectPassState with UserMessageStreak with IdleOperatorPingsEnabled with Closed
        with OnRevisit {
          override def initialStreakPinged: Boolean = false
          override def emulateMoreUnprocessedMessages: Boolean = false

          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
          override def expectCBMessages: Seq[CBMessage] = Seq.empty
          override def expectChatClosed: Boolean = true
        }

      "send idle operator pings when the time is right" in
        new Stage2 with DirectPassState with UserMessageStreak with IdleOperatorPingsEnabled with OnRevisit
        with ExpectStreakInState {
          override lazy val initialStreakTime: Instant =
            clock.instant().minus(OfferChatIdlePingProcessor.DurationUntilIdleOperatorPing.plusMillis(1))
          override def emulateMoreUnprocessedMessages: Boolean = false
          override def initialStreakPinged: Boolean = false

          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
          override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MIdleOperatorPing"))
          override def expectStreakStart: Instant = initialStreakTime
          override def expectStreakPinged: Boolean = true
        }

      "requeue pings when time is not right" in
        new Stage2 with DirectPassState with UserMessageStreak with IdleOperatorPingsEnabled with OnRevisit {
          override lazy val initialStreakTime: Instant = clock.instant().minusSeconds(60)
          override def emulateMoreUnprocessedMessages: Boolean = false
          override def runBeforeProcessing(): Unit = {
            super.runBeforeProcessing()
            IdleOperatorPingQueue.expectEnqueue()
          }

          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
          override def expectCBMessages: Seq[CBMessage] = Seq.empty
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            IdleOperatorPingQueue.enqueueAfter.value shouldBe
              initialStreakTime.plus(OfferChatIdlePingProcessor.DurationUntilIdleOperatorPing)
          }
        }

      "requeue pings when the right time comes at night" in
        new Stage2 with DirectPassState with UserMessageStreak with IdleOperatorPingsEnabled with OnRevisit {
          override lazy val initialStreakTime: Instant = Instant.from(
            LocalDateTime
              .of(2022, 5, 27, NightAutoresponse.NightStartHour, 0, 0)
              .minusSeconds(1)
              .atZone(TimeUtils.MSK)
          )

          override def emulateMoreUnprocessedMessages: Boolean = false
          override def runBeforeProcessing(): Unit = {
            clock.setInstant(initialStreakTime.plus(OfferChatIdlePingProcessor.DurationUntilIdleOperatorPing))
            super.runBeforeProcessing()
            IdleOperatorPingQueue.expectEnqueue()
          }

          override def expectBotState: StateCase = StateCase.DIRECT_PASS
          override def expectBotStateToRestore: StateCase = StateCase.STATE_NOT_SET
          override def expectCBMessages: Seq[CBMessage] = Seq.empty
          override def checkAfterProcessing(): Unit = {
            super.checkAfterProcessing()
            IdleOperatorPingQueue.enqueueAfter.value shouldBe
              Instant.from(LocalDateTime.of(2022, 5, 28, NightAutoresponse.NightEndHour, 0, 0).atZone(TimeUtils.MSK))
          }
        }

    }

    "in stage 2 state FAQ2" should {

      trait WantToRent extends OnMessage {
        this: Fixture =>
        override def eventMessageKeyboardResponse: Option[String] =
          Some(BotConfig.A_FAQ_WANT_TO_RENT_FIELD_NUMBER.toString)
      }

      "move to next question" in
        new Stage2 with Faq2State with WantToRent with InitAppData with OnMessage with ExpectMoveToLongTermOnly {
          override def initAppData(adb: ApplicationData.Builder): Unit = {}
          override def expectReturningMessage: Boolean = true
        }

      "move to some further question" in
        new Stage2 with Faq2State with WantToRent with InitAppData with OnMessage with ExpectMoveToTenants {
          override def initAppData(adb: ApplicationData.Builder): Unit = adb.getLongTermOnlyBuilder
          override def expectReturningMessage: Boolean = true
        }

    }

  }

}
