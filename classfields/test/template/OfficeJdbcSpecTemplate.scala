package ru.auto.salesman.test.template

import org.slf4j.{Logger, LoggerFactory}
import ru.auto.salesman.dao.impl.jdbc.database.Database

trait OfficeJdbcSpecTemplate extends JdbcSpecTemplateBase {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def url: String = MySqlContainer.container.getJdbcUrl
  def user: String = MySqlContainer.container.getUsername
  def password: String = MySqlContainer.container.getPassword
  def driver: String = "com.mysql.jdbc.Driver"
  def schemaScript: String = "/sql/office.final.sql;/sql/data/office.sql"

  lazy val database: Database =
    createDatabase(
      "autoru_salesman_office_unit_test",
      url,
      user,
      password,
      driver,
      schemaScript
    )
}
