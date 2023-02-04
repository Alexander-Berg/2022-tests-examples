package ru.yandex.vertis.karp.dao.impl

import cats.implicits.catsSyntaxOptionId
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import org.scalacheck.magnolia.gen
import ru.yandex.vertis.karp.dao.TaskDao
import ru.yandex.vertis.karp.model._
import ru.yandex.vertis.karp.model.Arbitraries._
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isTrue}
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object YdbTaskDaoSpec extends DefaultRunnableSpec {

  private lazy val daoLayer = Clock.any >+> ydb >+> TransactionSupport.live >+> TaskDao.live

  private val task = generate[AutoruTask].sample.get

  override def spec: ZSpec[TestEnvironment, Any] = {
    import ru.yandex.vertis.karp.dao.TaskDao._
    (suite("YdbTaskDao")(
      testM("upsert") {
        val result = transactionally(upsert(task))
        assertM(result.as(true))(isTrue)
      },
      testM("get") {
        val result = transactionally(get(task.id))
        assertM(result)(equalTo(task.some))
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(daoLayer)
  }
}
