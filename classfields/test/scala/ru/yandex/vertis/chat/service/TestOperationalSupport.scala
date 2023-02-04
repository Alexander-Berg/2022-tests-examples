package ru.yandex.vertis.chat.service

import ru.yandex.vertis.chat.components.operational.OperationalAware
import ru.yandex.vertis.ops
import ru.yandex.vertis.ops.OpsFactory

/**
  * Provides metrics awareness for testing purposes.
  *
  * @author dimas
  */
trait TestOperationalSupport extends OperationalAware {
  val operational: ops.OperationalSupport = OpsFactory.newOperationalSupport
}
