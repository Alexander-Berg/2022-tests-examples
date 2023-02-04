package ru.auto.salesman.test.template

trait SalesmanUserJdbcSpecTemplate extends JdbcSpecTemplateBase {

  private def url: String = MySqlContainer.container.getJdbcUrl
  private def user: String = MySqlContainer.container.getUsername
  private def password: String = MySqlContainer.container.getPassword
  private def driver: String = "com.mysql.jdbc.Driver"
  private def schemaScript: String = "/sql/salesman.user.final.sql"

  lazy val database =
    createDatabase(
      "autoru_salesman_user_unit_test",
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
