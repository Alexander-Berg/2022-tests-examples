package vertis.pushnoy.util

import ru.yandex.pushnoy.EventLogModel
import vertis.pushnoy.util.event.EventWriter
import vertis.pushnoy.util.log.base.EventLog

/** @author kusaeva
  */
class TestEventWriter extends EventWriter {
  override def logEvent(eventLog: EventLog, message: EventLogModel.PushSentEvent): Unit = ()
}
