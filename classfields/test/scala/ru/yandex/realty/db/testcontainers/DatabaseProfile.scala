package ru.yandex.realty.db.testcontainers

import slick.jdbc.JdbcProfile

trait DatabaseProfile {
  def jdbcProfile: JdbcProfile
}

trait MySQLDatabaseProfile extends DatabaseProfile {
  override lazy val jdbcProfile: JdbcProfile = slick.jdbc.MySQLProfile
}

trait PostgreSQLDatabaseProfile extends DatabaseProfile {
  override lazy val jdbcProfile: JdbcProfile = slick.jdbc.PostgresProfile
}
