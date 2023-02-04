package ru.yandex.vos2.autoru

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.application.deploy.Deploys
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.baker.env.EnvProvider

/**
  * Created by andrey on 8/16/16.
  */
object TestEnvProvider extends EnvProvider {
  override def environmentType: Environments.Value = Environments.Development

  override def props: Config = {
    def load(resource: String): Config = {
      ConfigFactory.parseResources(resource)
    }
    ConfigFactory.load(
      ConfigFactory.empty
        .withFallback(load("test.local.conf"))
        .withFallback(load("test.conf"))
        .withFallback(load("properties.testing.conf"))
        .withFallback(load("properties.development.conf"))
        .withFallback(load("properties.local.conf"))
        .withFallback(load("properties.conf"))
        .withFallback(load("application.conf"))
    )
  }

  override def dataCenter: String = ""

  override def deployType: Deploys.Value = Deploys.Debian
}
