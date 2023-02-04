package ru.yandex.vertis.chat.service.impl.jdbc

import org.scalatest.concurrent.ScalaFutures
import ru.yandex.vertis.chat.components.dao.chat.storage.DatabaseStorage
import ru.yandex.vertis.chat.service.impl.TestDomainAware
import ru.yandex.vertis.chat.util.logging.Logging
import slick.jdbc.JdbcBackend.DatabaseDef

trait JdbcSpec extends ScalaFutures with Logging {

  //should be lazy due strange immediate instantiation of all test in maven-surefire-plugin
  lazy val database: DatabaseStorage = DatabaseStorage(JdbcSpec.database, JdbcSpec.database)

}

object JdbcSpec extends TestDomainAware {

  private def schemaPath: String = "./sql/schema.sql"

  //should be lazy due strange immediate instantiation of all test in maven-surefire-plugin
  lazy val database: DatabaseDef =
    TestDockerConfigBuilder.createDatabase("chat_autoru", schemaPath, domain)
}
