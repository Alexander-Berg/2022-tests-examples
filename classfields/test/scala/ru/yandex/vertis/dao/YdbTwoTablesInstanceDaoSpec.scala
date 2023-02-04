package ru.yandex.vertis.dao

import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

class YdbTwoTablesInstanceDaoSpec extends InstanceDaoSpecBase {

  override val schemaFileName: String = "/schema_two_tables.sql"

  override lazy val instanceDao: InstanceDao[F] = new YdbTwoTablesInstanceDao[F, WithTransaction[F, *]](Service.GENERAL, ydbWrapper)
}
