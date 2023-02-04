package ru.yandex.realty.db.testcontainers

import org.testcontainers.containers.JdbcDatabaseContainer
import ru.yandex.realty.db.testcontainers.TestContainer.ContainerConfig

/**
  * Add the following properties to your `resources/testcontainers.properties` file:
  * {{{
  *   ryuk.container.image = testcontainersofficial/ryuk:<version>
  *   ...
  * }}}
  */
trait TestContainer {
  this: DatabaseProfile =>

  def containerConfig: ContainerConfig

  def container: JdbcDatabaseContainer[_]
}

object TestContainer {
  case class ContainerConfig(
    databaseName: String,
    databasePort: Int,
    databaseUser: String,
    databasePassword: String
  )
}
