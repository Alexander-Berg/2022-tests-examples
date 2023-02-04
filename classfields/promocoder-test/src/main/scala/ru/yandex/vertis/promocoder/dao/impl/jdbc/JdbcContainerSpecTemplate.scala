package ru.yandex.vertis.promocoder.dao.impl.jdbc

import org.scalatest.{BeforeAndAfterAll, Suite}
import ru.yandex.vertis.promocoder.AsyncSpecBase

/** Provides database for unit tests
  *
  * @author alex-kovalenko
  */
trait JdbcContainerSpecTemplate extends BeforeAndAfterAll with JdbcContainerSpec with AsyncSpecBase {
  this: Suite =>

  def username: String = "root"
  def password: String = "test"
  def driver: String = "com.mysql.jdbc.Driver"
  def dropAfterExecution: Boolean = true

  val schemaPath = "/sql/promocoder.final.sql"

  lazy val database =
    createDatabase("promocoder_unit_test", driver, username, password, schemaPath, drop = dropAfterExecution)
}
