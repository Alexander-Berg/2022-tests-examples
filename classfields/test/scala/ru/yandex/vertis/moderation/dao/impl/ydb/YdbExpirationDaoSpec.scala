package ru.yandex.vertis.moderation.dao.impl.ydb

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.YdbSpecBase
import ru.yandex.vertis.moderation.dao.{ExpirationDao, ExpirationDaoSpecBase}
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

@RunWith(classOf[JUnitRunner])
class YdbExpirationDaoSpec extends ExpirationDaoSpecBase with YdbSpecBase {

  override val resourceSchemaFileName: String = "/expiration-dao.sql"

  override lazy val expirationDao: ExpirationDao = new YdbExpirationDao[F, WithTransaction[F, *]](ydbWrapper)
}
