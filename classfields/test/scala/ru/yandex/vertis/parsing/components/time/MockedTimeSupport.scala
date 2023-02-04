package ru.yandex.vertis.parsing.components.time

import org.joda.time.DateTime
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedTimeSupport extends TimeAware with MockitoSupport {
  override val timeService: TimeService = mock[TimeService]
  when(timeService.getNow).thenReturn(DateTime.now().withMillisOfDay(0))
}
