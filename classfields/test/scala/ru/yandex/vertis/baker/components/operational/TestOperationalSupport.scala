package ru.yandex.vertis.baker.components.operational

import ru.yandex.vertis.ops

trait TestOperationalSupport extends OperationalAware {
  implicit val operational: ops.OperationalSupport = ops.test.TestOperationalSupport
}
