package ru.yandex.vertis.vsquality.techsupport.service.bot.impl

import cats.Id
import cats.kernel.Monoid
import com.softwaremill.tagging._
import org.scalatest.Assertions
import ru.yandex.vertis.vsquality.techsupport.model._
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.{Action, BasicAction, SimpleInternalScenario}
import ru.yandex.vertis.vsquality.techsupport.service.bot.CompositeScenario.{CompositeContext, CompositeState}
import ru.yandex.vertis.vsquality.techsupport.service.bot.ExternalScenario.{ExternalContext, Replacements}
import ru.yandex.vertis.vsquality.techsupport.service.bot.{BotScenario, CompositeScenario, ExternalScenario}
import ru.yandex.vertis.vsquality.techsupport.util.{scenarioFromFile, SpecBase, StubModerationScenario}
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson._
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatInstances._

/**
  * @author potseluev
  */
class CompositeScenarioSpec extends SpecBase {

  import CompositeScenarioSpec._

  private val greetingScenarioId: ScenarioId.External =
    ScenarioId.External("greeting_scenario".taggedWith[Tags.ExternalScenarioId])
  private val greetingScenario: ExternalScenario = scenarioFromFile("greeting_scenario.json")

  private val chooseScenarioId: ScenarioId.External =
    ScenarioId.External("choose_scenario".taggedWith[Tags.ExternalScenarioId])
  private val chooseScenario: ExternalScenario = scenarioFromFile("choose_scenario.json")
  private val fromModerationReplacements: Map[String, String] = Map("additional_info" -> "полезная информация")

  private val withUserIdReplacement: Replacements = Map(
    "user-id" -> "Анфиса"
  )

  private val moderationScenario: SimpleInternalScenario[Id] = new StubModerationScenario(
    Action.Switch(
      scenario = greetingScenarioId,
      stateId = Some("call_operator".taggedWith[Tags.BotStateId])
    ),
    fromModerationReplacements
  )

  private val internal2ExternalScenario: SimpleInternalScenario[Id] = new StubModerationScenario(
    Action.Switch(
      scenario = greetingScenarioId,
      stateId = Some("switch_to_choose".taggedWith[Tags.BotStateId])
    ),
    Map.empty
  )

  private val compositeContext: CompositeContext = mock[CompositeContext]
  when(compositeContext.replacements).thenReturn(withUserIdReplacement)
  when(compositeContext.os).thenReturn(None)
  when(compositeContext.appVersion).thenReturn(None)

  private val externalScenarios: Map[ScenarioId.External, ExternalScenario] = Map(
    greetingScenarioId -> greetingScenario,
    chooseScenarioId -> chooseScenario
  )

  private val internalScenarios: Map[ScenarioId.Internal, SimpleInternalScenario[Id]] = Map(
    ScenarioId.Internal.BlockedOffersModeration -> moderationScenario,
    ScenarioId.Internal.Reseller -> internal2ExternalScenario
  )

  val compositeScenario: CompositeScenario[Id] = new CompositeScenarioImpl(
    externalScenarios,
    internalScenarios,
    Map.empty,
    chooseScenarioId
  )

  def cmdToPayload(cmd: BotCommand): Message.Payload = {
    Message.Payload(cmd, Seq.empty, Seq.empty, None)
  }

  private def getAsBasicState(
      scenarioId: ScenarioId.External,
      stateId: String
    )(implicit ctx: ExternalContext): BotScenario.State[BasicAction] =
    externalScenarios(scenarioId)
      .getState(stateId.taggedWith[Tags.BotStateId])
      .get
      .ensureIsBasic

  "CompositeScenario" should {
    "switch to another external scenario correctly on transition" in {
      val command = "Бот, отвали".taggedWith[Tags.BotCommand]
      val actualCompositeState =
        compositeScenario.transit(compositeScenario.startFrom, cmdToPayload(command))(compositeContext)
      val expectedCompositeState = CompositeState(
        scenarioId = greetingScenarioId,
        state = getAsBasicState(greetingScenarioId, "call_operator")(Monoid[ExternalContext].empty)
      )
      actualCompositeState shouldBe Some(expectedCompositeState)
    }

    "switch to another external scenario by internal scenario correctly on transition" in {
      val command = "Расскажи про заблокированные офферы".taggedWith[Tags.BotCommand]
      val actualCompositeState =
        compositeScenario.transit(compositeScenario.startFrom, cmdToPayload(command))(compositeContext)
      val expectedCompositeState = CompositeState(
        scenarioId = greetingScenarioId,
        state = getAsBasicState(greetingScenarioId, "call_operator")(
          ExternalContext(
            fromModerationReplacements
          )
        )
      )
      actualCompositeState shouldBe Some(expectedCompositeState)
    }

    "apply replacements from CompositeContext on different transitions" in {

      val toInternalCommand = "Сделай что-нибудь внутри".taggedWith[Tags.BotCommand]
      val actualToInternalCompositeState =
        compositeScenario.transit(compositeScenario.startFrom, cmdToPayload(toInternalCommand))(compositeContext)
      val expectedToInternalCompositeState = CompositeState(
        scenarioId = chooseScenarioId,
        state = getAsBasicState(chooseScenarioId, "start")(
          ExternalContext(
            withUserIdReplacement
          )
        )
      )
      actualToInternalCompositeState shouldBe Some(expectedToInternalCompositeState)

      val moveToGreetingCommand = "Поприветствуй меня!".taggedWith[Tags.BotCommand]
      val actualMoveToGreetingCompositeState =
        compositeScenario.transit(actualToInternalCompositeState.get.id, cmdToPayload(moveToGreetingCommand))(
          compositeContext
        )
      val expectedMoveToGreetingCompositeState = CompositeState(
        scenarioId = greetingScenarioId,
        state = getAsBasicState(greetingScenarioId, "start")(compositeContext)
      )
      actualMoveToGreetingCompositeState shouldBe Some(expectedMoveToGreetingCompositeState)

      val moveToChooseCommand = "Давай к начальному диалогу".taggedWith[Tags.BotCommand]
      val actualMoveToChooseCompositeState =
        compositeScenario.transit(actualMoveToGreetingCompositeState.get.id, cmdToPayload(moveToChooseCommand))(
          compositeContext
        )
      val expectedMoveToChooseCompositeState = CompositeState(
        scenarioId = chooseScenarioId,
        state = getAsBasicState(chooseScenarioId, "start")(compositeContext)
      )
      actualMoveToChooseCompositeState shouldBe Some(expectedMoveToChooseCompositeState)

      val againMoveToGreetingCommand = "Поприветствуй меня!".taggedWith[Tags.BotCommand]
      val actualAgainMoveToGreetingCompositeState =
        compositeScenario.transit(actualToInternalCompositeState.get.id, cmdToPayload(againMoveToGreetingCommand))(
          compositeContext
        )
      val expectedAgainMoveToGreetingCompositeState = CompositeState(
        scenarioId = greetingScenarioId,
        state = getAsBasicState(greetingScenarioId, "start")(compositeContext)
      )
      actualAgainMoveToGreetingCompositeState shouldBe Some(expectedAgainMoveToGreetingCompositeState)

      val lastCommand = "Скажи мое имя".taggedWith[Tags.BotCommand]
      val actualLastCompositeState =
        compositeScenario.transit(actualAgainMoveToGreetingCompositeState.get.id, cmdToPayload(lastCommand))(
          compositeContext
        )
      val expectedLastCompositeState = CompositeState(
        scenarioId = greetingScenarioId,
        state = getAsBasicState(greetingScenarioId, "say_my_name")(compositeContext)
      )
      actualLastCompositeState shouldBe Some(expectedLastCompositeState)
    }

    "switch to another state correctly on getState" in {
      val stateId = CompositeStateId(
        scenarioId = compositeScenario.startFrom.scenarioId,
        stateId = "switch_to_greeting".taggedWith[Tags.BotStateId]
      )
      val actualCompositeState = compositeScenario.getState(stateId)(compositeContext)
      val expectedCompositeState = CompositeState(
        scenarioId = greetingScenarioId,
        state = getAsBasicState(greetingScenarioId, "start")(ExternalContext(withUserIdReplacement))
      )
      actualCompositeState shouldBe Some(expectedCompositeState)
    }

    "get non switch state as well" in {
      val stateId = CompositeStateId(
        scenarioId = greetingScenarioId,
        stateId = "say_name".taggedWith[Tags.BotStateId]
      )
      val actualCompositeState = compositeScenario.getState(stateId)(compositeContext)
      val expectedCompositeState = CompositeState(
        scenarioId = greetingScenarioId,
        state = getAsBasicState(greetingScenarioId, "say_name")(Monoid[ExternalContext].empty)
      )
      actualCompositeState shouldBe Some(expectedCompositeState)
    }
  }
}

object CompositeScenarioSpec {

  implicit private class RichState(val state: BotScenario.State[Action]) extends AnyVal {

    def ensureIsBasic: BotScenario.State[BasicAction] = state.action match {
      case basicAction: BasicAction => state.copy(action = basicAction)
      case other                    => Assertions.fail(s"Unexpected state $other")
    }
  }

}
