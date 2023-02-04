package common.db.migrations.test.migrations

import common.db.migrations.liquibase.ZTaskChange
import common.db.migrations.liquibase.ZTaskChange.TaskEnvironment
import liquibase.database.Database
import liquibase.database.jvm.JdbcConnection
import zio.{ZIO, ZLayer}

class TestCustomMigration extends ZTaskChange {

  override type Env = TaskEnvironment

  override def makeEnv: ZLayer[TaskEnvironment, Throwable, Env] = ZLayer.requires[TaskEnvironment]

  override def run(database: Database): ZIO[Env, Throwable, Any] = {
    ZIO.effect {
      val connection = database.getConnection.asInstanceOf[JdbcConnection].getWrappedConnection
      connection
        .prepareStatement(
          """INSERT INTO offers (id, seller_id, type) VALUES
                |(1, 110, 'SELL'),
                |(2, 140, 'RENT');
                |""".stripMargin.replaceAll("\n", " ")
        )
        .execute()
    }
  }
}
