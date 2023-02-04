package ru.yandex.vertis.caching.base

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.containers.GenericContainer

/**
  * @author korvit
  */
trait ContainerIntSpec
  extends Suite
    with BeforeAndAfterAll {

  protected def containerName: String

  protected lazy val container = new GenericContainer(containerName)
  container.start()
  sys.addShutdownHook(container.stop())

  abstract override def afterAll(): Unit = {
    container.stop()
    super.afterAll()
  }
}
