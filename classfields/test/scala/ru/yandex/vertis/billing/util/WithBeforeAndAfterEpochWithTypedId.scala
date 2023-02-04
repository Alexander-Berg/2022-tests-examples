package ru.yandex.vertis.billing.util

import ru.yandex.vertis.billing.model_core.EpochWithTypedId

/**
  * @author tolmach
  */
trait WithBeforeAndAfterEpochWithTypedId[T] {
  def before: EpochWithTypedId[T]
  def after: EpochWithTypedId[T]
}
