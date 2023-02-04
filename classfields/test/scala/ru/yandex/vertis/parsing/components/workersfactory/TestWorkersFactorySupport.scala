package ru.yandex.vertis.parsing.components.workersfactory

import ru.yandex.vertis.parsing.components.workersfactory.workers.{TestWorkersFactory, WorkersFactory}

/**
  * TODO
  *
  * @author aborunov
  */
trait TestWorkersFactorySupport extends WorkersFactoryAware {
  val workersFactory: WorkersFactory = new TestWorkersFactory
}
