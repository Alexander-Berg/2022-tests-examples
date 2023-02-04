package ru.yandex.vertis.parsing.components

import ru.yandex.vertis.parsing.lifecycle.{Application, TestApplication}

/**
  * TODO
  *
  * @author aborunov
  */
trait TestApplicationSupport extends ApplicationAware {
  override def app: Application = TestApplication
}
