package ru.yandex.vertis.cartinder.dao

import ru.auto.api.api_offer_model.{Category, OfferId}
import ru.yandex.vertis.cartinder.model.{Match, UserId}
import ru.yandex.vertis.cartinder.proto.api.MatchesRequest.Page
import common.zio.ydb.testkit.InitSchema
import common.zio.ydb.testkit.TestYdb._
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport.transactionally
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.Task
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{beforeAll, sequential}
import zio.test.environment.TestEnvironment

import java.time.Instant

object MatchDaoSpec extends DefaultRunnableSpec {

  private lazy val matchDaoLayer =
    ydb >>> MatchDao.live

  private val transactionSupport = ydb >>> TransactionSupport.live

  private val m = Match(
    OfferId(Category.CARS, "123"),
    UserId("123"),
    Instant.now,
    false,
    UserId("321"),
    OfferId(Category.CARS, "321")
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    import MatchDao._

    (suite("YdbMatchDaoImpl")(
      testM("upsert") {
        val res =
          transactionally(upsertMany(List(m)))
            .as(true)
            .orDieWith(_.squash)
        assertM(res)(isTrue)
      },
      testM("find match") {
        val res =
          transactionally(filterByUser(m.userId, None, Page(1, 1), m.created))
            .orDieWith(_.squash)
        assertM(res)(contains(m))
      },
      testM("not find non existing match") {
        val res =
          transactionally(filterByUser(m.userId, None, Page(1, 1), m.created.minusSeconds(1)))
            .orDieWith(_.squash)
        assertM(res)(isEmpty)
      }
    ) @@ sequential @@ beforeAll(InitSchema("/schema.sql").orDie))
      .provideCustomLayerShared(ydb ++ transactionSupport ++ Clock.live ++ matchDaoLayer)
  }
}
