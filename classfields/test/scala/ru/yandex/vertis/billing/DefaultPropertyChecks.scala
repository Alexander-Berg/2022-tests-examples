package ru.yandex.vertis.billing

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
  * @author ruslansd
  */
trait DefaultPropertyChecks extends ScalaCheckPropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100, workers = 5)
}
