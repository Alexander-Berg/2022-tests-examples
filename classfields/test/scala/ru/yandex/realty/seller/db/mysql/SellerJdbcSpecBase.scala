package ru.yandex.realty.seller.db.mysql

import org.scalatest.{OneInstancePerTest, Suite}
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase2
import ru.yandex.realty.db.DbSpecBase
import ru.yandex.realty.db.mysql.JdbcSpecBase
import slick.jdbc.JdbcProfile

trait SellerJdbcSpecBase extends JdbcSpecBase with DbSpecBase with OneInstancePerTest {
  self: Suite =>

  override type DbProfile = JdbcProfile

  implicit override lazy val database: Db =
    prepareDatabase("/sql/seller.final.sql", "realty_seller")
  lazy val database2: MasterSlaveJdbcDatabase2.DbWithProperties =
    MasterSlaveJdbcDatabase2.DbWithProperties(database, "realty_seller")
  lazy val masterSlaveDb2: MasterSlaveJdbcDatabase2 = MasterSlaveJdbcDatabase2(database2, database2)
}
