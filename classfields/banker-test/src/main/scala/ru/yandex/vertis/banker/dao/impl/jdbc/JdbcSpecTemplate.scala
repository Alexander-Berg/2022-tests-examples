package ru.yandex.vertis.banker.dao.impl.jdbc

import org.scalatest.Suite
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplateBase.Scheme
import slick.jdbc.JdbcBackend

trait JdbcSpecTemplate extends JdbcSpecTemplateBase {
  this: Suite =>

  lazy val database: JdbcBackend.Database = createNewDatabase

  def createNewDatabase: JdbcBackend.Database =
    create(Scheme("/sql/vs_billing_banker.final.sql", "vs_billing_banker_unit_test"))
}
