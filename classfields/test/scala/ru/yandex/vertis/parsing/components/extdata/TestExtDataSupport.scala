package ru.yandex.vertis.parsing.components.extdata

import ru.yandex.vertis.parsing.util.TestDataEngine
import ru.yandex.vertis.parsing.util.extdata.ExtDataEngine

/**
  * TODO
  *
  * @author aborunov
  */
trait TestExtDataSupport extends ExtDataAware {
  override protected def extDataEngine: ExtDataEngine = TestDataEngine
}
