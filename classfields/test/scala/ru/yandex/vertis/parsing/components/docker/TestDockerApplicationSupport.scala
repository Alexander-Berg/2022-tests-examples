package ru.yandex.vertis.parsing.components.docker

import ru.yandex.vertis.parsing.components.ApplicationAware
import ru.yandex.vertis.parsing.lifecycle.{Application, TestDockerApplication}

/**
  * TODO
  *
  * @author aborunov
  */
trait TestDockerApplicationSupport extends ApplicationAware {
  override def app: Application = TestDockerApplication
}
