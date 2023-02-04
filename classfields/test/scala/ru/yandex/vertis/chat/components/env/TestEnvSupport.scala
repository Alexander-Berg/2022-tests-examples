package ru.yandex.vertis.chat.components.env

/**
  * TODO
  *
  * @author aborunov
  */
trait TestEnvSupport extends EnvAware {
  val env: Env = new Env(TestEnvProvider)
}
