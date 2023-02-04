package ru.auto.tests.module

import java.io.{FileOutputStream, IOException}
import java.util.Properties

import com.google.inject.{AbstractModule, Provides, Singleton}
import org.aeonbits.owner.{Config, ConfigFactory}
import ru.auto.tests.config.CarfaxApiConfig

object CarfaxApiConfigModule {

  private val log =
    org.apache.log4j.Logger.getLogger(classOf[CarfaxApiConfigModule])
}

class CarfaxApiConfigModule extends AbstractModule {

  @Provides
  @Singleton
  def provideCabinetApiConfig: CarfaxApiConfig = {
    val config: CarfaxApiConfig =
      ConfigFactory.create(classOf[CarfaxApiConfig], System.getProperties.clone.asInstanceOf[Properties], System.getenv)

    val properties = new Properties
    config.fill(properties)

    val methods = classOf[CarfaxApiConfig].getDeclaredMethods
    val propertiesNames = methods.toStream
      .map(m => m.getDeclaredAnnotation(classOf[Config.Key]).value)
      .toSet

    properties.entrySet.removeIf(e => !propertiesNames.contains(e.getKey.toString))

    val path = "target/allure-results/environment.properties"

    try properties.store(new FileOutputStream(path), null)
    catch {
      case ignored: IOException =>
        CarfaxApiConfigModule.log.warn(String.format("%s not found", path))
    }

    config
  }

  override protected def configure(): Unit = {}
}
