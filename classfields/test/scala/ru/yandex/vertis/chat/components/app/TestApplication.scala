package ru.yandex.vertis.chat.components.app

import ru.yandex.vertis.chat.components.app.config.{AppConfig, AppConfigAware}
import ru.yandex.vertis.chat.components.env.TestEnvSupport

/**
  * TODO
  *
  * @author aborunov
  */
class TestApplication(val config: AppConfig) extends Application with TestEnvSupport with AppConfigAware
