package ru.auto.tests.module

import java.io.{FileOutputStream, IOException}
import java.util.Properties

import com.google.inject.{AbstractModule, Provides, Singleton}
import org.aeonbits.owner.{Config, ConfigFactory}
import org.apache.log4j.Logger.getLogger
import ru.auto.tests.config.GarageApiConfig

object GarageApiConfigModule {

  private val log = getLogger(classOf[GarageApiConfigModule])
}

class GarageApiConfigModule extends AbstractModule {

  @Provides
  @Singleton
  def provideCabinetApiConfig: GarageApiConfig = {
    val config: GarageApiConfig =
      ConfigFactory.create(classOf[GarageApiConfig], System.getProperties.clone.asInstanceOf[Properties], System.getenv)

    val properties = new Properties
    config.fill(properties)

    val methods = classOf[GarageApiConfig].getDeclaredMethods
    val propertiesNames = methods.toStream
      .map(m => m.getDeclaredAnnotation(classOf[Config.Key]).value)
      .toSet

    properties.entrySet.removeIf(e => !propertiesNames.contains(e.getKey.toString))

    val path = "target/allure-results/environment.properties"

    try properties.store(new FileOutputStream(path), null)
    catch {
      case ignored: IOException =>
        GarageApiConfigModule.log.warn(String.format("%s not found", path))
    }

    config
  }

  override protected def configure(): Unit = {}
}
