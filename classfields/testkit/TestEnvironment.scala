package ru.auto.api.testkit

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import ru.auto.api.app.Environment
import ru.yandex.vertis.application.deploy.Deploys

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 08.02.17
  */
object TestEnvironment extends Environment {
  override lazy val config: Config = ConfigFactory.load()

  override def serviceName: String = "autoru"

  override def componentName: String = "api-tests"

  override def version: String = "test"

  override def hostName: String = "localhost"

  override def instance: String = hostName

  override def dataCenter: String = "undefined"

  override def environmentType: String = "local"

  override lazy val serviceDataPath: File = new File(".")

  override lazy val dataPath: File = new File(".")

  override def deploy: Deploys.Value = Deploys.Debian
}
