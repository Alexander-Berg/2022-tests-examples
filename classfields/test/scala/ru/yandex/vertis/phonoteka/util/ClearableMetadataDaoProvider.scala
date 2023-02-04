package ru.yandex.vertis.phonoteka.util

import ru.yandex.vertis.phonoteka.dao.MetadataDao

trait ClearableMetadataDaoProvider {
  implicit protected def clearableMetadataDao[F[_]]: Clearable[MetadataDao[F]]
}
