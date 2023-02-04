package ru.yandex.realty.persistence.cassandra

import java.io.File
import java.util.Properties

import ru.yandex.realty.application._
import ru.yandex.realty.deployment.DevelopmentPropertiesUtil
import ru.yandex.realty.application.{ConfigRoot, PropertiesHolder}
import ru.yandex.realty.util.{Properties => HProperties}

import scala.collection.JavaConverters._

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 27.05.16
  */
trait PropertiesSetup extends HProperties {
  val configRoot: ConfigRoot = new ConfigRoot(new File("."))

  val resources =
    ConfigResolver.resolveProperties(configRoot, EnvironmentType.DEVELOPMENT, Components.COMMON_COMPONENTS.asJava)

  val properties: Properties =
    DevelopmentPropertiesUtil.replace(new PropertiesLoader().load(resources, new Properties()))
  PropertiesHolder.create(properties, EnvironmentType.DEVELOPMENT)
}
