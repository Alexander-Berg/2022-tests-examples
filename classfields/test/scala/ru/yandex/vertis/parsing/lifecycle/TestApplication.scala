package ru.yandex.vertis.parsing.lifecycle

import ru.yandex.vertis.parsing.env.{Env, TestEnvProvider}

/**
  * TODO
  *
  * @author aborunov
  */
object TestApplication extends Application {
  override val env: Env = new Env(TestEnvProvider)
}
