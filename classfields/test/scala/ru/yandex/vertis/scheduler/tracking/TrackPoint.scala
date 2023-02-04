package ru.yandex.vertis.scheduler.tracking

import org.joda.time.DateTime
import ru.yandex.vertis.scheduler.model.SchedulerInstance

/**
 * @author dimas
 */
case class TrackPoint(time: DateTime,
                      scheduler: SchedulerInstance,
                      event: TaskEvent.Value,
                      success: Boolean) {
  require(time != null, "Null time")
  require(event != null, "Null event")
}
