package ru.yandex.realty.util

import ru.yandex.vertis.application.runtime.RuntimeConfig

trait TestDataSettings {

  // Is used for operations, which we don't want to perform while regression tests launches
  def canDoSoftlyExpensiveThing(phone: String): Boolean

  // Is used for operations, which we want to perform only for manual tests
  def canDoExtremelyExpensiveThing(phone: String): Boolean
}

class TestDataSettingsImpl(private val runtimeConfig: RuntimeConfig) extends TestDataSettings {

  import TestDataSettingsImpl._

  override def canDoSoftlyExpensiveThing(phone: String): Boolean =
    runtimeConfig.isEnvironmentStable || phone != RegressionTestsPhone

  override def canDoExtremelyExpensiveThing(phone: String): Boolean =
    runtimeConfig.isEnvironmentStable || phone == ManualTestsPhone
}

object TestDataSettingsImpl {

  // This phone is used for users created by regression tests, so we DON'T DO some expensive things for such users
  // in testing environment (creating AmoCRM leads, for example), checking for this phone number equality.
  private val RegressionTestsPhone = "+79998883333"

  // This phone is used in the other way - if the user from testing environment has this phone number,
  // we DO some expensive things for such user (for example, Spectrum Data checks and Yang checks).
  private val ManualTestsPhone = "+79040000000"
}
