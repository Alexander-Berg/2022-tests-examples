package ru.yandex.vertis.picapica.ops

import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

/**
  * Introduces [[OperationalSupport]] into tests.
  *
  * @author dimas
  */
trait TestOps {
  implicit def ops: OperationalSupport = TestOperationalSupport
}
