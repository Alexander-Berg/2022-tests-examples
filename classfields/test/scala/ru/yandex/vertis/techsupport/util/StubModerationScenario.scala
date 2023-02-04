package ru.yandex.vertis.vsquality.techsupport.util

import cats.Id
import ru.yandex.vertis.vsquality.techsupport.service.bot.BotScenario.Action
import ru.yandex.vertis.vsquality.techsupport.service.bot.ExternalScenario.{ExternalContext, Replacements}
import ru.yandex.vertis.vsquality.techsupport.service.bot.ModerationScenario
import ru.yandex.vertis.vsquality.techsupport.service.bot.ModerationScenario.ModerationContext
import ru.yandex.vertis.vsquality.techsupport.service.bot.SingleStateSwitchScenario.State

/**
  * @author potseluev
  */
class StubModerationScenario(switch: Action.Switch, replacements: Replacements) extends ModerationScenario[Id] {

  override def process(implicit ctx: ModerationContext): Option[State[ExternalContext]] =
    Some(State(switch, ExternalContext(replacements)))
}
