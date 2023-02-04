package vsquality.complaints.storage.test

import common.zio.doobie.testkit.TestPostgresql
import doobie.util.transactor.Transactor
import vsquality.complaints.model._
import vsquality.complaints.storage.BlacklistDao
import vsquality.complaints.storage.postgresql.PgBlacklistDao
import zio.{Task, ZIO}
import zio.test.TestAspect.sequential
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import common.zio.doobie.syntax._

import java.time.Instant

object PgBlacklistDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val userId = "123"
    val userId1 = "1234"
    (suite("PgBlackListDao")(
      testM("ban user") {
        for {
          dao <- ZIO.service[BlacklistDao.Service]
          xa  <- ZIO.service[Transactor[Task]]
          rowsAffected <- dao
            .banUsers(List((userId, Domain.REALTY), (userId1, Domain.AUTORU)), Instant.now)
            .transactIO(xa)
          isBanned <- dao.isUserBanned(userId, Domain.REALTY).transactIO(xa)
        } yield assertTrue(rowsAffected == 2 && isBanned)
      },
      testM("unban user") {
        for {
          dao         <- ZIO.service[BlacklistDao.Service]
          xa          <- ZIO.service[Transactor[Task]]
          rowsDeleted <- dao.unbanUsers(List((userId, Domain.REALTY), (userId1, Domain.AUTORU))).transactIO(xa)
          isBanned    <- dao.isUserBanned(userId, Domain.REALTY).transactIO(xa)
        } yield assertTrue(rowsDeleted == 2) && assertTrue(!isBanned)
      }
    ) @@ sequential).provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> PgBlacklistDao.live
    )

  }
}
