package ru.yandex.vertis.subscriptions.backend.confirmation.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.backend.confirmation.UserPropertiesServiceSpec

/**
  * Specs on [[MemoryUserProperties]]
  */
@RunWith(classOf[JUnitRunner])
class MemoryUserPropertiesSpec extends UserPropertiesServiceSpec {
  protected val service = new MemoryUserProperties
}
