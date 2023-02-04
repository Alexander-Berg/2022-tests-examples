package ru.yandex.vertis.dao

import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

class YdbHorizontalUniformWithRelationsInstanceDaoSpec extends InstanceDaoSpecBase {

  override val schemaFileName: String = "/schema_horizontal_uniform.sql"

  override lazy val instanceDao: InstanceDao[F] = new YdbHorizontalUniformWithRelationsInstanceDao[F, WithTransaction[F, *]](Service.GENERAL, ydbWrapper)
}