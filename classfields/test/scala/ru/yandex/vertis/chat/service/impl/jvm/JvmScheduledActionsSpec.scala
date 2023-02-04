package ru.yandex.vertis.chat.service.impl.jvm

import ru.yandex.vertis.chat.components.dao.scheduledactions.ScheduledActions
import ru.yandex.vertis.chat.service.ScheduledActionsSpecBase

/**
  * Runnable spec on [[JvmScheduledActions]].
  *
  * @author dimas
  */
class JvmScheduledActionsSpec extends ScheduledActionsSpecBase {
  val scheduledActions: ScheduledActions = new JvmScheduledActions
}
