package ru.yandex.realty.features

/**
  * Stub implementation of [[Feature]] for tests
  *
  * @author abulychev
  */
class FeatureStub(enabled: Boolean = true, override val generation: Int = 1) extends Feature {
  def isEnabled: Boolean = enabled

  override def name: String = ???

  override def enabledByDefault: Boolean = ???

  override def lastToggled: Long = ???

  override def setNewState(newState: Boolean): Unit = ???

  override def setNewState(newState: Boolean, generation: Int): Unit = ???

  override def setGeneration(generation: Int): Unit = ???
}
