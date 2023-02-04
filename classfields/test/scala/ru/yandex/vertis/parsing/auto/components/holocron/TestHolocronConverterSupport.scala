package ru.yandex.vertis.parsing.auto.components.holocron

import ru.yandex.vertis.parsing.auto.components.clients.ClientsAware
import ru.yandex.vertis.parsing.auto.components.features.FeaturesAware
import ru.yandex.vertis.parsing.auto.converters.holocron.AutoHolocronConverterImpl
import ru.yandex.vertis.parsing.auto.converters.holocron.validation.ValidatedHolocronConverter
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.components.executioncontext.ExecutionContextAware
import ru.yandex.vertis.parsing.components.extdata.regions.RegionsAware
import ru.yandex.vertis.parsing.components.holocron.HolocronConverterAware
import ru.yandex.vertis.parsing.components.time.TimeAware
import ru.yandex.vertis.parsing.holocron.HolocronConverter

/**
  * TODO
  *
  * @author aborunov
  */
trait TestHolocronConverterSupport
  extends HolocronConverterAware[ParsedRow]
  with MockedHolocronValidationSupport
  with ClientsAware
  with ExecutionContextAware
  with TimeAware
  with FeaturesAware
  with RegionsAware {

  val holocronConverter: HolocronConverter[ParsedRow] =
    new AutoHolocronConverterImpl(searcherClient, timeService, regionTree)
      with ValidatedHolocronConverter
      with HolocronValidationAwareImpl
      with ExecutionContextAwareImpl
      with FeaturesAwareImpl
}
