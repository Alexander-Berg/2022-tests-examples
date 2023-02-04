package ru.yandex.vertis.scheduler.impl.zk

import ru.yandex.vertis.scheduler.LockManagerSpec
import ru.yandex.vertis.scheduler.logging.LoggingLockManagerMixin

/**
 * Runnable spec on [[ru.yandex.vertis.scheduler.impl.zk.ZkLockManager]].
 *
 * @author dimas
 */
class ZkLockManagerIntSpec
  extends LockManagerSpec
  with ZooKeeperAware {
  val lockManager = new ZkLockManager(curatorBase, "/path/to/locks-1")
    with LoggingLockManagerMixin
}