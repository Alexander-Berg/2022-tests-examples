package ru.yandex.vertis.karp.dao.impl

import cats.implicits.catsSyntaxOptionId
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb.ydb
import ru.yandex.vertis.karp.dao.TaskIdempotencyKeyDao
import ru.yandex.vertis.karp.model._
import ru.yandex.vertis.karp.model.Arbitraries._
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.clock.Clock
import zio.test.Assertion.{equalTo, isTrue}
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object YdbTaskIdempotencyKeyDaoSpec extends DefaultRunnableSpec {

  private lazy val daoLayer = Clock.any >+> ydb >+> TransactionSupport.live >+> TaskIdempotencyKeyDao.live

  private val idempotencyKey = generate[IdempotencyKey].sample.get
  private val otherIdempotencyKey = generate[IdempotencyKey].sample.get
  private val taskId = generate[TaskId].sample.get

  override def spec: ZSpec[TestEnvironment, Any] = {
    import ru.yandex.vertis.karp.dao.TaskIdempotencyKeyDao._
    (suite("YdbTaskIdempotencyKeyDao")(
      testM("upsert") {
        val result = transactionally(upsert(idempotencyKey, taskId))
        assertM(result.as(true))(isTrue)
      },
      testM("get some") {
        val result = transactionally(get(idempotencyKey))
        assertM(result)(equalTo(taskId.some))
      },
      testM("get none") {
        val result = transactionally(get(otherIdempotencyKey))
        assertM(result)(equalTo(None))
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie)).provideCustomLayerShared(daoLayer)
  }
}
