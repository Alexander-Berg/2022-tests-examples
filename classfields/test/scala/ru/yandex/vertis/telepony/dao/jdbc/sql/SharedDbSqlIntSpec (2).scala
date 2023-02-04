package ru.yandex.vertis.telepony.dao.jdbc.sql

import org.scalatest.Ignore

/**
  *
  */
@Ignore
// TODO remove ignore
class SharedDbSqlIntSpec extends JdbcCreateSqlIntSpec {
  override def sqlBaseDir: String = "/sql/shared"
}
