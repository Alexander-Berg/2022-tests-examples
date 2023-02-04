package ru.auto.salesman.test.template

import org.slf4j.{Logger, LoggerFactory}
import ru.auto.salesman.dao.impl.jdbc.database.Database

trait MainJdbcSpecTemplate extends JdbcSpecTemplateBase {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def url: String = MySqlContainer.container.getJdbcUrl
  def user: String = MySqlContainer.container.getUsername
  def password: String = MySqlContainer.container.getPassword
  def driver: String = "com.mysql.jdbc.Driver"
  def officeSchemaScript: String = "/sql/office.final.sql;/sql/data/office.sql"
  def poiSchemaScript: String = "/sql/poi7.final.sql;/sql/data/poi7.sql"

  protected val office7: String = nextDatabaseName(
    "autoru_salesman_office_unit_test"
  )
  protected val poi7: String = nextDatabaseName("autoru_salesman_poi_unit_test")

  lazy val database: Database = {
    createDatabase(
      office7,
      url,
      user,
      password,
      driver,
      officeSchemaScript,
      addRandomPrefix = false
    )
    createDatabase(
      poi7,
      url,
      user,
      password,
      driver,
      poiSchemaScript,
      addRandomPrefix = false,
      returnInstance = true
    )
  }
}
