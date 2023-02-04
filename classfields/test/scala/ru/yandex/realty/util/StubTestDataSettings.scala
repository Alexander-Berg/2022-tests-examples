package ru.yandex.realty.util

object StubTestDataSettings extends TestDataSettings {
  override def canDoSoftlyExpensiveThing(phone: String): Boolean = true
  override def canDoExtremelyExpensiveThing(phone: String): Boolean = true
}
