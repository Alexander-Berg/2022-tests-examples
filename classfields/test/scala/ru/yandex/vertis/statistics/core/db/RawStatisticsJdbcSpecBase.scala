package ru.yandex.vertis.statistics.core.db

import org.scalatest.{OneInstancePerTest, Suite}
import ru.yandex.vertis.statistics.db.DbSpecBase
import ru.yandex.vertis.statistics.db.mysql.JdbcSpecBase
import slick.jdbc.JdbcProfile

/**
  * @author azakharov
  */
trait RawStatisticsJdbcSpecBase extends JdbcSpecBase with DbSpecBase with OneInstancePerTest {
  self: Suite =>

  override type DbProfile = JdbcProfile

  implicit override lazy val database: Db =
    prepareDatabase("/sql/mysql_schema.sql", "realty_cabinet_stat")

}
