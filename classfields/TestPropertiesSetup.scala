package ru.yandex.realty.util

import java.io.File
import java.util.{Properties => jProperties}

import ru.yandex.realty.RealtyCommonConfigs
import ru.yandex.realty.application._
import ru.yandex.realty.deployment._

import scala.collection.JavaConverters._

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 27.05.16
  */
trait TestPropertiesSetup extends Properties {
  val configRoot: ConfigRoot = new ConfigRoot(new File("."))

  val resources = ConfigResolver.resolveProperties(
    configRoot,
    EnvironmentType.DEVELOPMENT,
    List(
      RealtyCommonConfigs.VERTIS_DATASOURCES,
      RealtyCommonConfigs.EXTDATA_TYPES_NEW,
      RealtyCommonConfigs.CORE,
      RealtyCommonConfigs.GRAPHITE,
      RealtyCommonConfigs.ZOOKEEPER,
      RealtyCommonConfigs.HADOOP,
      RealtyCommonConfigs.METROFINDER,
      RealtyCommonConfigs.MONITORING
    ).asJava
  )

  val properties: jProperties =
    DevelopmentPropertiesUtil.replace(new PropertiesLoader().load(resources, new jProperties()))
  PropertiesHolder.create(properties, EnvironmentType.DEVELOPMENT)
}
