package ru.auto.tests.module

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import lombok.extern.log4j.Log4j
import org.aeonbits.owner.Config.Key
import org.aeonbits.owner.{Config, ConfigFactory}
import ru.auto.tests.config.SalesmanApiConfig
import java.io.FileOutputStream
import java.io.IOException
import java.util
import java.util.Properties
import java.util.stream.Collectors

object SalesmanApiConfigModule {

  private val log =
    org.apache.log4j.Logger.getLogger(classOf[SalesmanApiConfigModule])
}

class SalesmanApiConfigModule extends AbstractModule {

  @Provides
  @Singleton
  def provideCabinetApiConfig: SalesmanApiConfig = {
    val config: SalesmanApiConfig = ConfigFactory.create(
      classOf[SalesmanApiConfig],
      System.getProperties.clone.asInstanceOf[Properties],
      System.getenv
    )

    val properties = new Properties
    config.fill(properties)

    val methods = classOf[SalesmanApiConfig].getDeclaredMethods
    val propertiesNames = methods.toStream
      .map(m => m.getDeclaredAnnotation(classOf[Config.Key]).value)
      .toSet

    properties.entrySet.removeIf(e => !propertiesNames.contains(e.getKey.toString))

    val path = "target/allure-results/environment.properties"

    try properties.store(new FileOutputStream(path), null)
    catch {
      case ignored: IOException =>
        SalesmanApiConfigModule.log.warn(String.format("%s not found", path))
    }

    config
  }

  override protected def configure(): Unit = {}
}
