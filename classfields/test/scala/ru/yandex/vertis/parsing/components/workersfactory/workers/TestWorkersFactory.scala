package ru.yandex.vertis.parsing.components.workersfactory.workers

import scala.concurrent.duration.FiniteDuration

/**
  * TODO
  *
  * @author aborunov
  */
class TestWorkersFactory extends WorkersFactory {

  override def newCyclicAction(
      name: String,
      ordered: Boolean,
      initialDelay: FiniteDuration,
      failureDelay: FiniteDuration
  )(f: => WorkResult): Unit = {
    f
  }
}
