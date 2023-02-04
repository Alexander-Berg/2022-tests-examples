package ru.yandex.vertis.billing.dao.impl.jdbc

import org.apache.commons.dbcp2.BasicDataSource
import slick.jdbc.JdbcBackend.Database

/**
 * @author ruslansd
 */
trait JdbcConnector {

  protected def port: Int

  protected def url: String = s"jdbc:mysql://mysql.dev.vertis.yandex.net:$port"

  protected def userName = "analyst"

  protected def password = "analyst"

  protected def driver = "com.mysql.jdbc.Driver"

  protected def databaseName: String

  lazy val database = {
    val ds = {
      val ds = new BasicDataSource
      ds.setDriverClassName(driver)
      ds.setUrl(s"$url/$databaseName")
      ds.setUsername(userName)
      ds.setPassword(password)
      ds.setConnectionProperties(JdbcContainerSpec.DefaultConnectionProps)
      ds
    }
    Database.forDataSource(ds, Some(ds.getMaxTotal))
  }

  lazy val dualDatabase = DualDatabase(database, database)

}
