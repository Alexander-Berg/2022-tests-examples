package ru.yandex.vertis.chat.service.impl.jdbc

import ru.yandex.vertis.chat.components.dao.scheduledactions.ScheduledActions
import ru.yandex.vertis.chat.service.ScheduledActionsSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Runnable spec on [[JdbcScheduledActions]].
  *
  * @author dimas
  */
class JdbcScheduledActionsSpec extends ScheduledActionsSpecBase with JdbcSpec {

  val scheduledActions: ScheduledActions =
    new JdbcScheduledActions(database)
}
