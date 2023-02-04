package ru.yandex.vos2.reviews

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vos2.reviews.utils.DockerReviewCoreComponentsBuilder
import ru.yandex.vos2.util.environment.EnvProvider

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-01-17.
  */
class DockerReviewsEnvProvider extends EnvProvider {

  private val configString: String = DockerReviewCoreComponentsBuilder.createConfig

  override val environmentType: String = "development"

  override lazy val props: Config = {
    def load(resource: String): Config = {
      ConfigFactory.parseResources(resource)
    }

    ConfigFactory.load(
      ConfigFactory.parseString(configString)
        .withFallback(load("properties.development.conf"))
        .withFallback(load("properties.testing.conf"))
        .withFallback(load("properties.conf"))
    )
  }

}
