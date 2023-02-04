package ru.yandex.realty.componenttest.extdata.core

import ru.yandex.extdata.core.event.impl.{ContainerEventListenerImpl, DelayedEventListener}
import ru.yandex.extdata.core.logging.LoggingContainerEventListener

trait ExtdataEventListenerProvider {

  def listener: DelayedEventListener

}

trait ComponentTestExdataEventListenerProvider extends ExtdataEventListenerProvider {

  override lazy val listener: DelayedEventListener =
    new ContainerEventListenerImpl with LoggingContainerEventListener with DelayedEventListener

}
