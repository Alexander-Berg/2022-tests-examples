package ru.yandex.realty.pos

import ru.yandex.realty.ops.OperationalComponents
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

/**
  * Default provider for operational stuff.
  *
  * @author dimas
  */
trait TestOperationalComponents extends OperationalComponents {
  def ops: OperationalSupport = TestOperationalSupport
}
