package ru.yandex.vertis.cartinder.dao

import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb._
import ru.auto.api.api_offer_model.{Category, OfferId}
import ru.yandex.vertis.cartinder.model.{Reaction, ReactionKind, UserId}
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{beforeAll, sequential}
import zio.test.environment.TestEnvironment

import java.time.Instant

object ReactionDaoSpec extends DefaultRunnableSpec {

  private lazy val reactionDaoLayer =
    ydb >>> ReactionDao.live

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
    import ReactionDao._

    (suite("YdbReactionDaoImpl")(
      testM("upsert") {
        val res =
          transactionally(upsertMany(List(reaction)))
            .as(true)
            .orDieWith(_.squash)
        assertM(res)(isTrue)
      },
      testM("find reaction") {
        val res =
          transactionally(filterByOffer(reaction.offerId, None))
            .orDieWith(_.squash)
        assertM(res)(contains(reaction))

      },
      testM("not find non existing reaction") {
        val res =
          transactionally(filterByOffer(reaction.offerId, Some(UserId("NON_EXISTENT"))))
            .orDieWith(_.squash)
        assertM(res)(isEmpty)
      },
      testM("delete reaction") {
        val res =
          transactionally(deleteByOffer(reaction.offerId)).flatMap(_ =>
            transactionally(filterByOffer(reaction.offerId, None))
          )
        assertM(res)(isEmpty)
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie))
      .provideCustomLayerShared(ydb ++ transactionSupport ++ Clock.live ++ reactionDaoLayer)
  }
}
