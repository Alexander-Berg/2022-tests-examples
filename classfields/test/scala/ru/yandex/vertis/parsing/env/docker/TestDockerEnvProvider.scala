package ru.yandex.vertis.parsing.env.docker

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.parsing.env.EnvProvider
import ru.yandex.vertis.parsing.util.ConfigUtils._

/**
  * Created by andrey on 11/8/17.
  */
object TestDockerEnvProvider extends EnvProvider {

  private val containerName = {
    load("test.conf")
      .optString("parsing.test-container-name")
      .getOrElse(sys.error("no parsing-test-container-name property"))
  }

  private val testMysqlConfigNode = {
    load("test.conf").optString("parsing.test-node").getOrElse(sys.error("no parsing-test-node property"))
  }

  private val config = TestDockerConfigBuilder.createConfig(containerName, testMysqlConfigNode)

  override def environmentType: Environments.Value = Environments.Development

  override def dataCenter: String = "undefined"

  private def load(resource: String): Config = {
    ConfigFactory.parseResources(resource)
  }

  override val props: Config = {
    ConfigFactory.load(
      ConfigFactory
        .parseString(config)
        .withFallback(load("test.local.conf"))
        .withFallback(load("test.conf"))
        .withFallback(load("test_core.conf"))
        .withFallback(load("properties.development.conf"))
        .withFallback(load("properties.testing.conf"))
        .withFallback(load("properties.conf"))
        .withFallback(load("application.conf"))
    )
  }
}
