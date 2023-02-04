package auto.c2b.carp.storage.test

import auto.c2b.carp.storage.postgresql.ApplicationsDao
import auto.c2b.carp.storage.postgresql.impl.ApplicationsDaoImpl
import auto.c2b.carp.storage.testkit.Generators
import auto.c2b.common.model.paging.PagingRequest
import auto.c2b.common.testkit.CommonGenerators
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._

object ApplicationsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("ApplicationsDao")(
      testM("создаёт заявку") {
        checkNM(1)(Generators.Application.applicationAny) { original =>
          for {
            dao <- ZIO.service[ApplicationsDao.Service]
            _ <- dao.upsert(original).transactIO
            fromDb <- dao.get(original.id).transactIO
          } yield assert(fromDb)(isSome(equalTo(original)))
        }
      },
      testM("возвращает заявку c select for update") {
        checkNM(1)(Generators.Application.applicationAny) { application =>
          for {
            dao <- ZIO.service[ApplicationsDao.Service]
            _ <- dao.upsert(application).transactIO
            saved <- dao.getAndLock(application.id).transactIO
          } yield assert(saved)(isSome(equalTo(application)))
        }
      },
      testM("возвращает заявку") {
        checkNM(1)(Generators.Application.applicationAny) { application =>
          for {
            dao <- ZIO.service[ApplicationsDao.Service]
            _ <- dao.upsert(application).transactIO
            saved <- dao.get(application.id).transactIO
          } yield assert(saved)(isSome(equalTo(application)))
        }
      },
      testM("возвращает список заявок") {
        checkNM(1)(Gen.setOfN(10)(Generators.Application.applicationAny), CommonGenerators.userIdAny.derive) {
          (applications, userId) =>
            val withUser = applications.map(_.copy(userId = userId))
            for {
              dao <- ZIO.service[ApplicationsDao.Service]
              _ <- ZIO.foreach_(withUser)(dao.upsert(_).transactIO)
              got <- dao.list(userId, PagingRequest(10, 1)).transactIO
            } yield assert(got.applications)(hasSameElements(withUser.toList)) && assertTrue(
              got.page.total == withUser.size
            )
        }
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> ApplicationsDaoImpl.live
      }
  }
}
