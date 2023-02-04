package ru.auto.salesman.test.template

import ru.auto.salesman.dao.impl.jdbc.database.Database

trait SalesmanJdbcSpecTemplate extends JdbcSpecTemplateBase {

  def url: String = MySqlContainer.container.getJdbcUrl
  def user: String = MySqlContainer.container.getUsername
  def password: String = MySqlContainer.container.getPassword
  def driver: String = "com.mysql.jdbc.Driver"
  def schemaScript: String = "/sql/salesman.final.sql;/sql/data/trade_in.sql"

  lazy val database: Database =
    createDatabase(
      "autoru_salesman_sales_unit_test",
      url,
      user,
      password,
      driver,
      schemaScript
    )

  lazy val transactor =
    createTransactor(
      url,
      user,
      password,
      driver,
      database.databaseName
    )
}
