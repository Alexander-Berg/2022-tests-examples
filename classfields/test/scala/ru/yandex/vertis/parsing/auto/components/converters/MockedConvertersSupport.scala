package ru.yandex.vertis.parsing.auto.components.converters

import ru.yandex.vertis.mockito.MockitoSupport._
import ru.yandex.vertis.parsing.auto.converters.ImportConverter

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedConvertersSupport extends ConvertersAware {
  override val importConverter: ImportConverter = mock[ImportConverter]
}
