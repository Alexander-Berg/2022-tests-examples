package ru.yandex.vertis.vsquality.techsupport.service

import cats.Id
import cats.data.NonEmptyList
import cats.kernel.Monoid
import cats.syntax.applicative._
import com.softwaremill.tagging._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.vsquality.utils.feature_registry_utils.FeatureRegistryF
import ru.yandex.vertis.vsquality.utils.lang_utils.Use
import ru.yandex.vertis.vsquality.techsupport.model.Conversation.{BotMeta, MetadataSet}
import ru.yandex.vertis.vsquality.techsupport.model.Message.Payload
import ru.yandex.vertis.vsquality.techsupport.model._
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.Action.CompleteConversation
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.{Action, BasicAction, State}
import ru.yandex.vertis.vsquality.techsupport.service.bot.CompositeScenario.{CompositeContext, CompositeState}
import ru.yandex.vertis.vsquality.techsupport.service.bot.TechsupportBot.Verdict.{Escalate, Reply}
import ru.yandex.vertis.vsquality.techsupport.service.bot.TechsupportBot._
import ru.yandex.vertis.vsquality.techsupport.service.bot.{BotScenario, CompositeScenario, TechsupportBot}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable.AwaitableSyntax
import BasicFeatureTypes._
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.TechsupportBotImpl

/**
  * @author potseluev
  */
class TechsupportBotSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  implicit private val rc: ClientRequestContext = generate[ClientRequestContext]()
  implicit private val ac: AppealContext = generate[AppealContext]()
  private val scenario: CompositeScenario[Id] = mock[CompositeScenario[Id]]

  private val currentState =
    BotScenario.State("bot-state-id".taggedWith[Tags.BotStateId], BotScenario.Action.Reply("reply-msg", Seq()), Seq())

  implicit private val featureRegistry: FeatureRegistryF[F] =
    new FeatureRegistryF(new InMemoryFeatureRegistry(BasicFeatureTypes))

  private def bot(scenario: CompositeScenario[Id] = scenario): TechsupportBot[Id] =
    TechsupportBotImpl[Id, F](
      () => scenario.pure[Id],
      Settings(fallbackMessage = "Зову на помощь человека")
    ).await

  "TechsupportBot" should {
    "use startFrom state of scenario if botMetadata is not provided" in {
      val state = generate[State[Action.Reply]]()
      val startState = generate[CompositeState]().copy(state = state)
      val startStateId = startState.id
      stub(() => scenario.startFrom)(startState.id)
      stub(scenario.getState(_: CompositeStateId)(_: CompositeContext)) { case (`startStateId`, _) => Some(startState) }
      val conversation: Conversation = generate[Conversation]().copy(metadataSet = Monoid[MetadataSet].empty)
      val actualResult = bot(scenario).process(conversation)
      val expectedMeta = Metadata(rc.processingTime, BotMeta(startState.id))
      actualResult.newBotMetadata shouldBe Use(expectedMeta)
      actualResult.verdict match {
        case Verdict.Reply(message) =>
          message.payload.map(_.text).getOrElse("") shouldBe state.action.message
          message.payload.map(_.availableBotCommands).getOrElse(Seq.empty) shouldBe state.availableCommands
          message.author shouldBe UserId.Operator.VertisTechsupportBot
          message.`type` shouldBe MessageType.Common
          message.timestamp shouldBe rc.processingTime
        case other => fail(s"Unexpected $other")
      }
    }

    "transit to new state specified by provided botCommand" in {
      val botMeta = generate[Metadata[BotMeta]]()
      val botCommand = generate[BotCommand]()
      val payload = Payload(botCommand, Seq.empty, Seq.empty, None)
      val commandMessage = generate[Message]().copy(
        payload = Some(payload)
      )
      val messages = generate[NonEmptyList[Message]]() :+ commandMessage
      val conversation: Conversation =
        generate[Conversation]().copy(
          metadataSet = MetadataSet(Some(botMeta)),
          messages = messages
        )
      val action = generate[CompleteConversation]()
      val newState = generate[CompositeState]().copy(
        state = generate[State[BasicAction]]().copy(action = action)
      )
      stub(scenario.transit(_: CompositeStateId, _: Message.Payload)(_: CompositeContext)) {
        case (botMeta.value.stateId, `payload`, _) => Some(newState)
      }
      stub(scenario.getState(_: CompositeStateId)(_: CompositeContext)) { case (botMeta.value.stateId, _) =>
        Some(CompositeState(currentState, ScenarioId.External("scenario-id".taggedWith[Tags.ExternalScenarioId])))
      }
      val actualResult = bot(scenario).process(conversation)
      val expectedMeta = Metadata(rc.processingTime, BotMeta(newState.id))
      actualResult.newBotMetadata shouldBe Use(expectedMeta)
      actualResult.verdict match {
        case Verdict.CompleteConversation(messageToClient, needFeedback) =>
          needFeedback shouldBe action.needFeedback
          messageToClient.flatMap(_.payload.map(_.text)) shouldBe Some(action.message)
          messageToClient.map(_.timestamp) shouldBe Some(rc.processingTime)
        case other => fail(s"Unexpected $other")
      }
    }

    "escalate if can't transit to new state and retry feature is disabled" in {
      featureRegistry.updateFeature(TechsupportBotImpl.EnableRetryLastActionFeatureName, false).await
      val botMeta = generate[Metadata[BotMeta]]()
      val botCommand = generate[BotCommand]()
      val payload = Payload(botCommand, Seq.empty, Seq.empty, None)
      val commandMessage = generate[Message]().copy(
        payload = Some(payload)
      )
      val messages = generate[NonEmptyList[Message]]() :+ commandMessage
      val conversation: Conversation =
        generate[Conversation]().copy(
          metadataSet = MetadataSet(Some(botMeta)),
          messages = messages
        )
      stub(scenario.transit(_: CompositeStateId, _: Message.Payload)(_: CompositeContext)) {
        case (botMeta.value.stateId, `payload`, _) => None
      }
      stub(scenario.getState(_: CompositeStateId)(_: CompositeContext)) { case (botMeta.value.stateId, _) =>
        Some(CompositeState(currentState, ScenarioId.External("scenario-id".taggedWith[Tags.ExternalScenarioId])))
      }
      val actualResult = bot(scenario).process(conversation)
      actualResult.verdict shouldBe a[Escalate]
    }

    "reply if can't transit to new state and retry feature is enabled" in {
      featureRegistry.updateFeature(TechsupportBotImpl.EnableRetryLastActionFeatureName, true).await
      val botMeta = generate[Metadata[BotMeta]]()
      val botCommand = generate[BotCommand]()
      val payload = Payload(botCommand, Seq.empty, Seq.empty, None)
      val commandMessage = generate[Message]().copy(
        payload = Some(payload)
      )
      val messages = generate[NonEmptyList[Message]]() :+ commandMessage
      val conversation: Conversation =
        generate[Conversation]().copy(
          metadataSet = MetadataSet(Some(botMeta)),
          messages = messages
        )
      stub(scenario.transit(_: CompositeStateId, _: Message.Payload)(_: CompositeContext)) {
        case (botMeta.value.stateId, `payload`, _) => None
      }

      stub(scenario.getState(_: CompositeStateId)(_: CompositeContext)) { case (botMeta.value.stateId, _) =>
        Some(CompositeState(currentState, ScenarioId.External("scenario-id".taggedWith[Tags.ExternalScenarioId])))
      }
      val actualResult = bot(scenario).process(conversation)
      actualResult.verdict shouldBe a[Reply]
    }
  }

}
