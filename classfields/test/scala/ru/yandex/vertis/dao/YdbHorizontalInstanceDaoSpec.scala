package ru.yandex.vertis.dao

import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

class YdbHorizontalInstanceDaoSpec extends InstanceDaoSpecBase {

  override val schemaFileName: String = "/schema_horizontal.sql"

  override lazy val instanceDao: InstanceDao[F] = new YdbHorizontalInstanceDao[F, WithTransaction[F, *]](Service.GENERAL, ydbWrapper)
}