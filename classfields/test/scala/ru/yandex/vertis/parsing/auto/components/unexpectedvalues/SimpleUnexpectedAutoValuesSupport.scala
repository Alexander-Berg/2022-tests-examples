package ru.yandex.vertis.parsing.auto.components.unexpectedvalues

import ru.auto.api.ApiOfferModel
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.parsers.ParsedValue

trait SimpleUnexpectedAutoValuesSupport extends UnexpectedAutoValuesAware {

  override protected def checkParsedValue[T](name: String,
                                             value: ParsedValue[T],
                                             url: String,
                                             site: Site,
                                             category: ApiOfferModel.Category,
                                             source: CommonModel.Source): ParsedValue[T] = {
    value
  }
}
