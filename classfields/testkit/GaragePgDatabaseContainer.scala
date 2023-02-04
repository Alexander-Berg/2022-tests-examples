package ru.yandex.auto.garage.testkit

import auto.carfax.common.storages.pg.dao.users.UsersTable
import auto.carfax.common.storages.pg.{PgSlickDatabase, PgSlickMasterSlaveDatabase, PostgresProfile}
import auto.carfax.common.utils.logging.Logging
import auto.carfax.common.utils.tracing.Traced
import com.typesafe.config.ConfigFactory
import org.testcontainers.containers.PostgreSQLContainer
import ru.yandex.auto.garage.dao.cards.GarageCardsTable
import ru.yandex.auto.garage.dao.events.EventsQueueTable
import auto.carfax.common.storages.pg.PostgresProfile.api.{Database => SlickDatabase}
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait GaragePgDatabaseContainer extends Logging {

  def databaseSchemaList = {
    Seq(
      GarageCardsTable.cards,
      UsersTable.users,
      EventsQueueTable.events
    ).map(_.schema).reduce(_ ++ _)
  }

  def createDb: DBIOAction[Unit, NoStream, Effect.Schema] = {
    DBIO.seq(
      GarageCardsTable.primaryIdSeq,
      GarageCardsTable.encryptFunction,
      databaseSchemaList.create
    )
  }

  implicit private val t: Traced = Traced.empty

  implicit private val m: MetricsSupport = TestOperationalSupport

  private val dollar = "$$"

  private val pgContainer: PostgreSQLContainer[_] =
    new PostgreSQLContainer("postgres:11")

  private lazy val slickDatabase: PostgresProfile.backend.Database = {
    pgContainer.start()
    val psConfig =
      s"""postgresql {
         |    url = "${pgContainer.getJdbcUrl}"
         |
         |    properties {
         |      user = "${pgContainer.getUsername}"
         |      password = "${pgContainer.getPassword}"
         |    }
         |  }""".stripMargin
    log.info(
      s"Pg test container[${pgContainer.getJdbcUrl}] " +
        s"with u[${pgContainer.getUsername}]/p[${pgContainer.getPassword}]"
    )
    val db = SlickDatabase.forConfig("postgresql", ConfigFactory.parseString(psConfig))
    Await.result(db.run(createDb), 2.minute)
    db
  }

  private lazy val master = new PgSlickDatabase(slickDatabase, "master")
  private lazy val slave = new PgSlickDatabase(slickDatabase, "master")
  lazy val database: PgSlickMasterSlaveDatabase = PgSlickMasterSlaveDatabase(master, slave)
}
