package ru.yandex.vertis.subscriptions

import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Seconds, Span}

/**
  * Mixin for slow async specs
  *
  * @author dimas
  */
trait SlowAsyncSpec extends PatienceConfiguration {

  /**
    * Default value for futures [[PatienceConfig]].
    */
  private val DefaultPatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(50, Millis))

  implicit override def patienceConfig: PatienceConfig =
    DefaultPatienceConfig
}
