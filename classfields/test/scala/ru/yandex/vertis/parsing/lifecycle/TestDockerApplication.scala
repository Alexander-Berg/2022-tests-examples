package ru.yandex.vertis.parsing.lifecycle

import ru.yandex.vertis.parsing.env.Env
import ru.yandex.vertis.parsing.env.docker.TestDockerEnvProvider

/**
  * Created by andrey on 11/8/17.
  */
object TestDockerApplication extends Application {
  override val env: Env = new Env(TestDockerEnvProvider)
}
