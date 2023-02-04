package ru.auto.salesman.test.template

import org.slf4j.{Logger, LoggerFactory}
import ru.auto.salesman.dao.impl.jdbc.database.Database

trait BadgeJdbcSpecTemplate extends JdbcSpecTemplateBase {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def url: String = MySqlContainer.container.getJdbcUrl
  def user: String = MySqlContainer.container.getUsername
  def password: String = MySqlContainer.container.getPassword
  def driver: String = "com.mysql.jdbc.Driver"

  def salesScript: String =
    "/sql/categorized.sales.final.sql;/sql/data/categorized.sales.sql;/sql/sales.final.sql;/sql/data/sales.sql"

  def badgeDataScript: String = "/sql/data/badges.sql"

  lazy val database: Database =
    createDatabase(
      "autoru_salesman_sales_unit_test",
      url,
      user,
      password,
      driver,
      s"$salesScript;$badgeDataScript"
    )
}
