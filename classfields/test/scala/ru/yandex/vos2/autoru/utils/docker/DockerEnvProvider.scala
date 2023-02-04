package ru.yandex.vos2.autoru.utils.docker

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.application.deploy.Deploys
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.baker.env.EnvProvider

/**
  * Created by andrey on 11/10/16.
  */
object DockerEnvProvider extends EnvProvider {
  private val config = DockerAutoruCoreComponentsBuilder.createConfig
  override def environmentType: Environments.Value = Environments.Development

  override def props: Config = {
    def load(resource: String): Config = {
      ConfigFactory.parseResources(resource)
    }
    ConfigFactory.load(
      ConfigFactory
        .parseString(config)
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

  override def deployType: Deploys.Value = Deploys.Container
}
