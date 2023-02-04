package ru.yandex.realty.componenttest.env.config

import com.typesafe.config.impl.ConfigImpl

trait ComponentTestConfigBuilder[T] extends ComponentTestConfigProvider[T] {

  protected def buildConfig(): T

  override def config: T = {
    ConfigImpl.reloadSystemPropertiesConfig()
    buildConfig()
  }

}
