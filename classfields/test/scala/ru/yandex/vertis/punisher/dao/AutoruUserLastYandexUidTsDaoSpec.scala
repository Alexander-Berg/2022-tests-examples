package ru.yandex.vertis.punisher.dao

import java.time.Instant
import java.time.temporal.ChronoUnit

import ru.yandex.vertis.punisher.dao.impl.ydb.YdbBaseSpec
import ru.yandex.vertis.punisher.dao.impl.ydb.clearable.Clearable
import ru.yandex.vertis.quality.cats_utils.Awaitable._

trait AutoruUserLastYandexUidTsDaoSpec extends YdbBaseSpec {

  type Dao <: AutoruUserLastYandexUidTsDao[F]
  def dao: Dao
  def clearable: Clearable[Dao]

  before {
    clearable.clear()
  }

  "AutoruUserLastYandexUidTsDao" should {

    val now = Instant.now()
    val tomorrow = now.plus(1, ChronoUnit.DAYS)
    val yesterday = now.minus(1, ChronoUnit.DAYS)
    val userId = "1"

    "insert entry if no record" in {
      assume(dao.getLastYandexUidDateTime(userId).await === None)
      dao.upsertNewerTsOrIgnore(userId, now).await
      dao.getLastYandexUidDateTime(userId).await shouldBe Some(now)
    }

    "update timestamp if new ts > old ts" in {
      assume {
        dao.upsertNewerTsOrIgnore(userId, now).await
        dao.getLastYandexUidDateTime(userId).await === Some(now)
      }
      dao.upsertNewerTsOrIgnore(userId, tomorrow).await
      dao.getLastYandexUidDateTime(userId).await shouldBe Some(tomorrow)
    }

    "not update entry if old ts > new ts" in {
      assume {
        dao.upsertNewerTsOrIgnore(userId, now).await
        dao.getLastYandexUidDateTime(userId).await === Some(now)
      }
      dao.upsertNewerTsOrIgnore(userId, yesterday).await
      dao.getLastYandexUidDateTime(userId).await shouldBe Some(now)
    }

    "read multiple user ids" in {
      val userId1 = "1"
      val userId2 = "2"
      val userId3 = "3"

      dao.upsertNewerTsOrIgnore(userId1, yesterday).await
      dao.upsertNewerTsOrIgnore(userId2, now).await
      dao.upsertNewerTsOrIgnore(userId3, tomorrow).await

      val expected = Map(userId1 -> yesterday, userId2 -> now, userId3 -> tomorrow)
      dao.getLastYandexUidDateTime(Set(userId1, userId2, userId3)).await shouldBe expected
    }
  }
}
