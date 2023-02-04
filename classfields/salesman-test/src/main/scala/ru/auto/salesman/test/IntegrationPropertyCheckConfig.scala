package ru.auto.salesman.test

import org.scalatest.prop.Configuration

/** Provides property check config for integration tests.
  * This config allows to run tests faster, and pass tests after just one successful invocation.
  * It's alternative to calling next() everywhere.
  */
trait IntegrationPropertyCheckConfig extends Configuration {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1)
}
