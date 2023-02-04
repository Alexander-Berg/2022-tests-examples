package ru.yandex.vertis.scheduler.impl.jvm

import ru.yandex.vertis.scheduler.TaskServiceSpec

/**
 * Runnable spec on [[JvmTaskService]]
 *
 * @author dimas
 */
class JvmTaskServiceSpec
  extends TaskServiceSpec {
  val taskService = new JvmTaskService(getDescriptors)
}
