package ru.yandex.vertis.parsing.components.unexpectedvalues

import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.parsers.ParsedValue

/**
  * TODO
  *
  * @author aborunov
  */
trait SimpleUnexpectedValuesSupport extends UnexpectedValuesAware {

  override protected def checkParsedValue[T](name: String,
                                             value: ParsedValue[T],
                                             url: String,
                                             site: Site,
                                             category: String,
                                             source: CommonModel.Source): Option[T] = {
    value.toOption
  }
}
