package auto.c2b.reception.storage.test

import auto.c2b.common.postgresql.{QueueDao, QueueDaoImpl, QueueRow}
import cats.syntax.traverse.toTraverseOps
import common.zio.doobie.syntax.toConnectionIOExt
import common.zio.doobie.testkit.TestPostgresql
import doobie.free.connection.AsyncConnectionIO
import zio.ZIO
import zio.test.Assertion.{equalTo, hasSize}
import zio.test.TestAspect.sequential
import zio.test.{assert, assertTrue, AssertionResult, BoolAlgebra, DefaultRunnableSpec, ZSpec}

import scala.util.Random

object QueueDaoSpec extends DefaultRunnableSpec {

  private def assertRowsEqual(left: Seq[QueueRow], right: Seq[QueueRow]): BoolAlgebra[AssertionResult] = {
    assert(left)(hasSize(equalTo(right.size: Int))) && {
      left
        .zip(right)
        .map { case (l, r) =>
          assertTrue(l == r.copy(data = l.data)) &&
          assert(l.data)(equalTo(r.data)) // arrays' content cannot be checked other way
        }
        .reduce(_ && _)
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("QueueDao")(
      testM("Checks the push-pull-remove flow") {
        val input = List.range(1, 11).map(QueueRow(_, Random.nextBytes(10)))
        for {
          dao <- ZIO.service[QueueDao]
          _ <- input.map(dao.push).sequence.transactIO
          pulled1 <- dao.pull(3).transactIO
          _ <- dao.remove(pulled1.map(_.id)).transactIO
          pulled2 <- dao.pull(2).transactIO
          _ <- dao.remove(pulled2.map(_.id)).transactIO
          pulled3 <- dao.pull(10).transactIO
        } yield {
          assertRowsEqual(pulled1, input.take(3)) &&
          assertRowsEqual(pulled2, input.drop(3).take(2)) &&
          assertRowsEqual(pulled3, input.drop(5))
        }
      }
    ) @@ sequential)
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> QueueDaoImpl.live
      }
  }
}
