package ru.yandex.vertis.chat.components

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.chat.SpecBase

trait ComponentsSpecBase extends SpecBase {
  def testConfig: Config = ConfigFactory.empty()

}
