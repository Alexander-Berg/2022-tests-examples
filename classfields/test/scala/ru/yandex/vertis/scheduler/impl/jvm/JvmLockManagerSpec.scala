package ru.yandex.vertis.scheduler.impl.jvm

import ru.yandex.vertis.scheduler.LockManagerSpec

/**
 * Runnable spec on [[ru.yandex.vertis.scheduler.impl.jvm.JvmLockManager]].
 *
 * @author dimas
 */
class JvmLockManagerSpec
  extends LockManagerSpec {
  val lockManager = new JvmLockManager
}
