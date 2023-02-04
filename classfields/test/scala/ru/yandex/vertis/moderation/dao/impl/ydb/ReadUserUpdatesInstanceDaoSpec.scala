package ru.yandex.vertis.moderation.dao.impl.ydb

import ru.yandex.vertis.moderation.YdbSpecBase
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.{YdbCacheSerDe, YdbInstanceArchiveSerDe}
import ru.yandex.vertis.moderation.dao.{FuturedCache, ReadUserUpdatesInstanceDaoSpecBase, UserUpdatesCache}
import ru.yandex.vertis.moderation.model.instance.{ExternalId, Instance}
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

class ReadUserUpdatesInstanceDaoSpec extends ReadUserUpdatesInstanceDaoSpecBase with YdbSpecBase {
  override val resourceSchemaFileName: String = "/user-updates.sql"

  private val tableName = "user_updates"

  private val serDe =
    new YdbCacheSerDe[ExternalId, Instance] with YdbInstanceArchiveSerDe {
      implicit override def keySerDe: PrimitiveSerDe[ExternalId] = ExternalIdSerDe
      implicit override def valueSerDe: PrimitiveSerDe[Instance] = InstanceSerDe
    }

  lazy val dao: YdbCache[F, WithTransaction[F, *], ExternalId, Instance] =
    new YdbCache[F, WithTransaction[F, *], ExternalId, Instance](tableName, ydbWrapper, serDe)

  override lazy val cache: UserUpdatesCache = new FuturedCache[F, ExternalId, Instance](dao)
}
