package ru.yandex.vertis.moderation.dao.impl.ydb

import cats.implicits.catsSyntaxParallelTraverse
import org.joda.time.Instant
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import ru.yandex.vertis.moderation.YdbSpecBase
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.dao.LockDao
import ru.yandex.vertis.moderation.model.instance.User
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class YdbLockDaoSpec extends YdbSpecBase {
  override val resourceSchemaFileName: String = "/locks.sql"

  private val mocc = mock[() => Instant]
  private val now = DateTimeUtil.now().toInstant

  lazy val dao: LockDao[F, User] =
    new YdbLockDao[F, WithTransaction[F, *], User](Service.AUTORU, ydbWrapper)(
      _.key,
      mocc
    )

  before {
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM locks;")).await)
  }

  "YdbLockDao" should {
    "acquire and release" in {
      when(mocc.apply()).thenReturn(now)
      val user1 = User.Yandex("1")
      val user2 = User.Yandex("2")
      val user3 = User.Yandex("3")

      List(user1, user2, user3)
        .parTraverse { user =>
          dao.acquireLock(user) *> dao.releaseLock(user)
        }
        .void
        .await
    }

    "not let acquire twice" in {
      when(mocc.apply()).thenReturn(now)
      val user1 = User.Yandex("1")
      (dao.acquireLock(user1) *> dao.acquireLock(user1)).attempt.await shouldBe Left(YdbLockDao.AlreadyLocked)
    }

    "let acquire twice after ttl time" in {
      when(mocc.apply()).thenReturn(now)
      val user1 = User.Yandex("1")

      dao.acquireLock(user1).await
      when(mocc.apply()).thenReturn(now.plus(org.joda.time.Duration.standardSeconds(5 * 60)))
      dao.acquireLock(user1).attempt.await shouldBe Left(YdbLockDao.AlreadyLocked)
      when(mocc.apply()).thenReturn(now.plus(org.joda.time.Duration.standardSeconds(5 * 60 + 1)))
      dao.acquireLock(user1).await
    }
  }
}
