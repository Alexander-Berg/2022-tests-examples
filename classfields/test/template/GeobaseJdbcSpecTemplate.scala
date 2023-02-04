package ru.auto.salesman.test.template

import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.dao.impl.jdbc.database.doobie.Transactor

trait GeobaseJdbcSpecTemplate extends JdbcSpecTemplateBase {

  def url: String = MySqlContainer.container.getJdbcUrl
  def user: String = MySqlContainer.container.getUsername
  def password: String = MySqlContainer.container.getPassword
  def driver: String = "com.mysql.jdbc.Driver"
  def schemaScript: String = "/sql/geobase.final.sql;/sql/data/geobase.sql"

  lazy val database: Database =
    createDatabase(
      "autoru_salesman_geo_unit_test",
      url,
      user,
      password,
      driver,
      schemaScript
    )

  lazy val transactor: Transactor =
    createTransactor(
      url,
      user,
      password,
      driver,
      database.databaseName
    )

}
