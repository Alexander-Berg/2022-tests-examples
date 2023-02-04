
package ru.yandex.vertis.dao

import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

class YdbNormalizedUniformInstanceDaoSpec extends InstanceDaoSpecBase {

  override val schemaFileName: String = "/schema_normalized_uniform_tables.sql"

  override lazy val instanceDao: InstanceDao[F] = new YdbNormalizedUniformTablesInstanceDao[F, WithTransaction[F, *]](Service.GENERAL, ydbWrapper)
}
