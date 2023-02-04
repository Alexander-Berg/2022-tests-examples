package ru.yandex.vertis.parsing.auto.components.holocron

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.components.holocron.validation.HolocronValidationAware
import ru.yandex.vertis.parsing.holocron.validation.{HolocronValidationResult, HolocronValidator}

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedHolocronValidationSupport extends HolocronValidationAware with MockitoSupport {
  val holocronValidator: HolocronValidator = mock[HolocronValidator]
  when(holocronValidator.validate(?, ?, ?)).thenReturn(HolocronValidationResult.Valid)

  val oldHolocronValidator: HolocronValidator = mock[HolocronValidator]
}
