package vsquality.complaints.storage.test

import common.zio.doobie.testkit.TestPostgresql
import doobie.util.transactor.Transactor
import vsquality.complaints.model.{Application, Context}
import vsquality.complaints.storage.AnalyticsDao
import vsquality.complaints.storage.postgresql.PgAnalyticsDao
import zio.{Task, ZIO}
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import common.zio.doobie.syntax._

object PgAnalyticsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("PgAnalyticstDao")(
      testM("upsert") {
        for {
          dao <- ZIO.service[AnalyticsDao.Service]
          xa  <- ZIO.service[Transactor[Task]]
          info  = Context("1", Some("112"), Some(Application.IOS_APP), Some("1"), Some(true))
          info2 = Context("12", Some("112"), None, None, None)
          rowsAffected <- dao.upsert(Seq(info, info2)).transactIO(xa)
          c1           <- dao.get("1").transactIO(xa)
        } yield assertTrue(rowsAffected == 2 && info == c1)
      }
    ).provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> PgAnalyticsDao.live
    )

  }
}
