package ru.yandex.vertis.dao

import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

class YdbBlobInstanceDaoSpec extends InstanceDaoSpecBase {

  override val schemaFileName: String = "/schema_blob.sql"

  override lazy val instanceDao: InstanceDao[F] = new YdbBlobInstanceDao[F, WithTransaction[F, *]](Service.GENERAL, ydbWrapper)
}
