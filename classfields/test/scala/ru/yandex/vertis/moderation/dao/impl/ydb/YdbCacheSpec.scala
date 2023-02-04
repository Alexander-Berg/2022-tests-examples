package ru.yandex.vertis.moderation.dao.impl.ydb

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.caching.base.AsyncCache
import ru.yandex.vertis.moderation.YdbSpecBase
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.{YdbCacheSerDe, YdbInstanceArchiveSerDe}
import ru.yandex.vertis.moderation.dao.{AsyncCacheSpecBase, FuturedCache}
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class YdbCacheSpec extends AsyncCacheSpecBase with YdbSpecBase {

  override val resourceSchemaFileName: String = "/user-updates.sql"

  private val tableName = "user_updates"

  before {
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM user_updates;")).await)
  }

  private val serDe =
    new YdbCacheSerDe[Key, Value] with YdbInstanceArchiveSerDe {
      implicit override def keySerDe: PrimitiveSerDe[Key] = ExternalIdSerDe
      implicit override def valueSerDe: PrimitiveSerDe[Value] = InstanceSerDe
    }

  lazy val dao: YdbCache[F, WithTransaction[F, *], Key, Value] =
    new YdbCache[F, WithTransaction[F, *], Key, Value](tableName, ydbWrapper, serDe)

  override lazy val asyncCache: AsyncCache[Key, Value] = new FuturedCache[F, Key, Value](dao)
}
