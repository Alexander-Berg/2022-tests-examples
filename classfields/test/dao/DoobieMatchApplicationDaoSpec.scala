package auto.dealers.match_maker.logic.dao

import common.zio.doobie.testkit.TestPostgresql._
import doobie.implicits._
import doobie.util.transactor.Transactor
import ru.auto.match_maker.model.api.ApiModel.MatchApplication
import common.zio.ops.database.TransactionContext
import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import zio.test.TestAspect._
import zio.test._

object DoobieMatchApplicationDaoSpec extends DefaultRunnableSpec {
  import DoobieMatchApplicationDaoSpecOps._

  def spec = {
    (DaoTestSuite
      .getDaoTestSuite("DoobieMatchApplicationDao") @@ before(initSchema.orDie) @@ sequential)
      .provideLayerShared(matchApplicationDao)
  }
}

object DoobieMatchApplicationDaoSpecOps {

  val matchApplicationDao: ZLayer[Blocking, Nothing, Has[MatchApplicationDao.Service] with Has[Transactor[zio.Task]]] = {
    val ref = FiberRef.make(TransactionContext.Empty).toLayer
    val dao = ZLayer.fromServices((xa: Transactor[zio.Task], ref: FiberRef[TransactionContext]) =>
      new DoobieMatchApplicationDao(xa, ref): MatchApplicationDao.Service
    )

    (managedTransactor ++ ref) >>> dao.passthrough
  }

  def initSchema: ZIO[Has[Transactor[zio.Task]], Throwable, Unit] = {
    ZIO.accessM { tr =>
      (for {
        _ <- sql"DROP TYPE IF EXISTS match_application_state cascade".update.run
        _ <- sql"DROP TABLE IF EXISTS processed_users_applications cascade".update.run
        _ <- sql"DROP TABLE IF EXISTS match_applications cascade".update.run
        _ <-
          sql"create type match_application_state as enum ('new', 'needs_processing', 'processed', 'busy')".update.run
        _ <- sql"""create table if not exists match_applications
                  |(
                  |	id varchar(36) not null
                  |		constraint match_applications_pk
                  |			primary key,
                  |	proto bytea not null,
                  |	last_modified timestamp not null,
                  |	created timestamp not null,
                  |	state match_application_state default 'new'::match_application_state not null,
                  |	expire_date timestamp not null,
                  |	user_id bigint not null
                  |);""".stripMargin.update.run
        _ <- sql"""create table if not exists processed_users_applications
                  |(
                  |	user_id bigint primary key,
                  |	start_timestamp timestamp not null
                  |)""".stripMargin.update.run
      } yield ()).transact(tr.get)
    }
  }

  def getApplicationWithId(id: String): MatchApplication =
    MatchApplication.newBuilder().setId(id).build()
}
