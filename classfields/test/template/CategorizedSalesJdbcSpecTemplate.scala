package ru.auto.salesman.test.template

import org.slf4j.{Logger, LoggerFactory}
import ru.auto.salesman.dao.impl.jdbc.database.Database

trait CategorizedSalesJdbcSpecTemplate extends JdbcSpecTemplateBase {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def url: String = MySqlContainer.container.getJdbcUrl
  def user: String = MySqlContainer.container.getUsername
  def password: String = MySqlContainer.container.getPassword
  def driver: String = "com.mysql.jdbc.Driver"

  def schemaScript: String =
    "/sql/categorized.sales.final.sql;/sql/data/categorized.sales.sql"

  lazy val database: Database =
    createDatabase(
      "autoru_salesman_categorized_sales_unit_test",
      url,
      user,
      password,
      driver,
      schemaScript
    )
}
