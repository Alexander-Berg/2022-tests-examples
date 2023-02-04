package ru.yandex.vertis.billing.banker.service.effect.util

import ru.yandex.vertis.billing.banker.service.effect.EffectExecutionContextAware
import ru.yandex.vertis.util.concurrent.Threads

import scala.concurrent.ExecutionContext

/**
  * Used to guarantee no races when testing effects
  */
trait TestingEffectExecutionContextAware extends EffectExecutionContextAware {

  implicit override protected val effectEc: ExecutionContext = {
    Threads.SameThreadEc
  }

}
