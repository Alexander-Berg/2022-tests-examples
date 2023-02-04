package ru.yandex.vos2.reviews.utils

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vos2.reviews.DockerReviewsEnvProvider
import ru.yandex.vos2.reviews.env.{DefaultEnvironment, DefaultReviewsEnvProvider, ReviewsEnv}
import ru.yandex.vos2.util.log.Logging

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 05/10/2017.
  */
class DockerReviewEnvironment extends ReviewsEnv(new DockerReviewsEnvProvider) with Logging {

  private lazy val configString: String = DockerReviewCoreComponentsBuilder.createConfig

  override lazy val environmentType: String = "development"

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
  override def version: String = "1"
  override def opsPort: Int = 1234
}

