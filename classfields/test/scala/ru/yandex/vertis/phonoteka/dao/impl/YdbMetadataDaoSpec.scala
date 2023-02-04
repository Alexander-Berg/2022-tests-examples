package ru.yandex.vertis.phonoteka.dao.impl

import ru.yandex.vertis.phonoteka.dao.{MetadataDao, MetadataDaoSpecBase}
import ru.yandex.vertis.phonoteka.util.YdbSpecBase
import ru.yandex.vertis.quality.cats_utils.MonadErr

class YdbMetadataDaoSpec extends MetadataDaoSpecBase with YdbSpecBase {
  implicit val metadataSerialization = new YdbMetadataSerialization[F]
  override def dao: MetadataDao[F] = new YdbMetadataDao(ydb)
}
