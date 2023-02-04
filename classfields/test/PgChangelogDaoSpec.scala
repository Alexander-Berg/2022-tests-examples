package infra.changelog.storage.test

import cats.effect.Blocker
import com.dimafeng.testcontainers.PostgreSQLContainer
import infra.changelog.storage.PgChangelogDao
import zio.test.Assertion.{contains, equalTo, hasSameElements, isEmpty, isSome}
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test._
import zio.interop.catz._
import common.zio.doobie.testkit.TestPostgresql
import common.zio.doobie.testkit.TestPostgresql.managedContainer
import doobie.util.transactor.Transactor
import doobie.free.connection
import org.postgresql.PGConnection
import scala.language.existentials
import java.sql.DriverManager
import infra.changelog.storage.model.Revision
import zio._
import zio.magic._
import zio.blocking.Blocking

object PgChangelogDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PgChangelogDao")(
      testM("get revisions") {
        val service = "test_service"
        val r3 = Revision(3, "frontender", "cool button", Nil)
        val r5 = Revision(5, "hacker", "increase timeout in bar", Seq("TICKET-42", "TICKET-43"))
        for {
          _ <- uploadTestData
          revisions <- PgChangelogDao.getRevisions(service, 1, 6)
        } yield assert(revisions)(hasSameElements(Seq(r3, r5)))
      }
    ) @@ after(PgChangelogDao.clean) @@ beforeAll(
      PgChangelogDao.initSchema.orDie
    ) @@ sequential
  }.provideCustomLayerShared(sharedLayer)

  private val sharedLayer =
    ZLayer.fromSomeMagic[Blocking, Has[PostgreSQLContainer] with Has[PgChangelogDao]](
      TestPostgresql.managedContainer,
      ZLayer.fromServices[PostgreSQLContainer, Blocking.Service, Transactor[Task]] { (container, blocking) =>
        Transactor.fromDriverManager[Task](
          container.driverClassName,
          container.jdbcUrl,
          container.username,
          container.password,
          Blocker.liftExecutionContext(blocking.blockingExecutor.asEC)
        )
      },
      ZLayer.fromService[Transactor[Task], PgChangelogDao](
        new PgChangelogDao(_)
      )
    )

  private val uploadTestData: RIO[Has[PostgreSQLContainer], Unit] =
    ZIO.service[PostgreSQLContainer] >>= { pg =>
      val c = pg.container
      val connection = DriverManager.getConnection(c.getJdbcUrl, c.getUsername, c.getPassword)
      val copyManager = connection.unwrap(classOf[PGConnection]).getCopyAPI
      val hashesReader = getClass.getResourceAsStream("/hashes.csv").ensuring(_ ne null, s"Hashes not found")
      val revisionsReader = getClass.getResourceAsStream("/revisions.csv").ensuring(_ ne null, s"Revisions not found")
      Task(copyManager.copyIn("COPY revisions FROM STDIN WITH CSV", revisionsReader)) *>
        Task(copyManager.copyIn("COPY hashes FROM STDIN WITH CSV", hashesReader)).unit
    }
}
