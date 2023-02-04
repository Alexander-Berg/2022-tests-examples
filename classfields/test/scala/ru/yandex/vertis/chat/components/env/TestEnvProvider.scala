package ru.yandex.vertis.chat.components.env

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.application.environment.Environments

/**
  * TODO
  *
  * @author aborunov
  */
object TestEnvProvider extends EnvProvider {
  override def environmentType: Environments.Value = Environments.Local

  override def dataCenter: String = "undefined"

  override def props: Config = {
    def load(resource: String): Config = {
      ConfigFactory.parseResources(resource)
    }
    ConfigFactory.load(
      ConfigFactory
        .load("test.local.conf")
        .withFallback(load("test.conf"))
        .withFallback(load("properties.development.conf"))
        .withFallback(load("properties.testing.conf"))
        .withFallback(load("properties.conf"))
//        .withFallback(load("application.conf"))
    )
  }

  override def allocation: Option[String] = None

  override def hostname: String = "localhost"
}
