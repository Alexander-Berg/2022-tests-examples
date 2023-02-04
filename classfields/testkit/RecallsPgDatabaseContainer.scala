package ru.yandex.auto.recalls.core.testkit

import auto.carfax.common.storages.pg.{PgSlickDatabase, PgSlickMasterSlaveDatabase, PostgresProfile}
import auto.carfax.common.utils.logging.Logging
import auto.carfax.common.utils.tracing.Traced
import com.github.tminglei.slickpg.PgEnumSupportUtils
import com.typesafe.config.ConfigFactory
import org.testcontainers.containers.PostgreSQLContainer
import ru.yandex.auto.recalls.core.dao.RecallsEventQueueTable
import ru.yandex.auto.recalls.core.db.{
  Campaign,
  Notification,
  Recall,
  RecallVinCode,
  UserCard,
  UserCardRecallState,
  VinCodesTable
}
import ru.yandex.auto.recalls.core.enums.NotificationStatus
import auto.carfax.common.storages.pg.PostgresProfile.api.{Database => SlickDatabase}
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

trait RecallsPgDatabaseContainer extends Logging {

  def databaseSchemaList = {
    Seq(
      RecallsEventQueueTable.events,
      Campaign.entries,
      Recall.entries,
      Notification.entries,
      UserCard.entries,
      UserCardRecallState.entries,
      VinCodesTable.entries,
      RecallVinCode.entries
    ).map(_.schema).reduce(_ ++ _)
  }

  private val enums = Seq(NotificationStatus)

  private val createEnums = DBIO.sequence(enums.map { enum =>
    PgEnumSupportUtils.buildCreateSql(enum.name, enum)
  })

  def createDb: DBIOAction[Unit, NoStream, Effect.Schema] = {
    DBIO.seq(
      databaseSchemaList.dropIfExists,
      createEnums,
      databaseSchemaList.create
    )
  }

  implicit val t: Traced = Traced.empty

  implicit private val m: MetricsSupport = TestOperationalSupport

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
