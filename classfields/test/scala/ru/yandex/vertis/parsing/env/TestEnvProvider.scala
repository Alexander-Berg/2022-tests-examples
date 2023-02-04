package ru.yandex.vertis.parsing.env

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.application.environment.Environments

/**
  * TODO
  *
  * @author aborunov
  */
object TestEnvProvider extends EnvProvider {
  override def environmentType: Environments.Value = Environments.Development

  override def dataCenter: String = "undefined"

  override val props: Config = {
    def load(resource: String): Config = {
      ConfigFactory.parseResources(resource)
    }
    ConfigFactory.load(
      ConfigFactory
        .load("test.local.conf")
        .withFallback(load("test.conf"))
        .withFallback(load("test_core.conf"))
        .withFallback(load("properties.development.conf"))
        .withFallback(load("properties.testing.conf"))
        .withFallback(load("properties.conf"))
        .withFallback(load("application.conf"))
    )
  }
}
