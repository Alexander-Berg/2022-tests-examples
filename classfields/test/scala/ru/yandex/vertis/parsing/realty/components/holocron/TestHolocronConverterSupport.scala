package ru.yandex.vertis.parsing.realty.components.holocron

import ru.yandex.vertis.parsing.components.executioncontext.ExecutionContextAware
import ru.yandex.vertis.parsing.components.holocron.HolocronConverterAware
import ru.yandex.vertis.parsing.components.time.TimeAware
import ru.yandex.vertis.parsing.holocron.HolocronConverter
import ru.yandex.vertis.parsing.realty.components.features.FeaturesAware
import ru.yandex.vertis.parsing.realty.dao.offers.ParsedRealtyRow
import ru.yandex.vertis.parsing.realty.holocron.{RealtyHolocronConverterImpl, ValidatedHolocronConverter}

/**
  * TODO
  *
  * @author aborunov
  */
trait TestHolocronConverterSupport
  extends HolocronConverterAware[ParsedRealtyRow]
  with MockedHolocronValidationSupport
  with ExecutionContextAware
  with FeaturesAware {

  val holocronConverter: HolocronConverter[ParsedRealtyRow] = new RealtyHolocronConverterImpl
    with ValidatedHolocronConverter
    with HolocronValidationAwareImpl
    with ExecutionContextAwareImpl
    with FeaturesAwareImpl
}
