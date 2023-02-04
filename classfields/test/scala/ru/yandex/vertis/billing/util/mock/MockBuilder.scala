package ru.yandex.vertis.billing.util.mock

import ru.yandex.vertis.mockito.MockitoSupport

/**
  * @author tolmach
  */
trait MockBuilder[T] extends MockitoSupport {

  def build: T

}
