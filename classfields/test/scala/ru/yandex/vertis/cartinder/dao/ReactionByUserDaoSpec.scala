package ru.yandex.vertis.cartinder.dao

import ru.auto.api.api_offer_model.{Category, OfferId}
import ru.yandex.vertis.cartinder.model.{Reaction, ReactionKind, UserId}
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb._
import ru.yandex.vertis.ydb.zio.{Tx, TxEnv, TxError, TxTask}
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.{Has, Task, URIO, ZIO}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{beforeAll, sequential}
import zio.test.environment.TestEnvironment

import java.time.Instant

object ReactionByUserDaoSpec extends DefaultRunnableSpec {

  private lazy val reactionDaoLayer =
    ydb >>> ReactionByUserDao.live

  private val transactionSupport = ydb >>> TransactionSupport.live

  private val reaction = Reaction(
    OfferId(Category.CARS, "123"),
    UserId("123"),
    ReactionKind.Like,
    Instant.now,
    UserId("321"),
    OfferId(Category.CARS, "321")
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    import ReactionByUserDao._

    (suite("YdbReactionByUserDaoImpl")(
      testM("upsert") {
        val res =
          transactionally(upsertMany(List(reaction)))
            .as(true)
            .orDieWith(_.squash)
        assertM(res)(isTrue)
      },
      testM("find reaction") {
        val res =
          transactionally(filterByUser(reaction.userId, None))
            .orDieWith(_.squash)
        assertM(res)(contains(reaction))
      },
      testM("not find non existing reaction") {
        val res =
          transactionally(filterByUser(UserId("NON_EXISTENT"), None))
            .orDieWith(_.squash)
        assertM(res)(isEmpty)
      },
      testM("delete reaction") {
        val res =
          deleteByShard(List(reaction))
            .flatMap { list =>
              ZIO
                .foreach(list)(tx => transactionally(tx))
                .flatMap(_ => transactionally(filterByUser(reaction.userId, None)))
                .orDieWith(_.squash)
            }
        assertM(res)(isEmpty)
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie))
      .provideCustomLayerShared(ydb ++ transactionSupport ++ Clock.live ++ reactionDaoLayer)
  }
}
