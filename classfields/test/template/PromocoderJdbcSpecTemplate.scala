package ru.auto.salesman.test.template

import ru.auto.salesman.dao.impl.jdbc.database.Database

trait PromocoderJdbcSpecTemplate extends JdbcSpecTemplateBase {

  def url: String = MySqlContainer.container.getJdbcUrl
  def user: String = MySqlContainer.container.getUsername
  def password: String = MySqlContainer.container.getPassword
  def driver: String = "com.mysql.jdbc.Driver"
  def schemaScript: String = "/sql/promocoder.final.sql"

  lazy val autoruDatabase: Database =
    createDatabase(
      "promocoder_autoru_unit_test",
      url,
      user,
      password,
      driver,
      schemaScript
    )

  lazy val realtyDatabase: Database =
    createDatabase(
      "promocoder_realty_unit_test",
      url,
      user,
      password,
      driver,
      schemaScript
    )

  lazy val autoruUsersDatabase: Database =
    createDatabase(
      "promocoder_autoru_users_unit_test",
      url,
      user,
      password,
      driver,
      schemaScript
    )
}
