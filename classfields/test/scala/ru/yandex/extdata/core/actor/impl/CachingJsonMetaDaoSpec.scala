package ru.yandex.extdata.core.actor.impl

import java.io.File

import ru.yandex.extdata.core.actor.{MetaDaoSpecBase, MetaStorageSpecBase}
import ru.yandex.extdata.core.storage.{MetaDao, MetaStorage}
import ru.yandex.extdata.core.storage.impl.{CachingMetaDao, JsonMetaDao}

/**
  * @author evans
  */
class CachingJsonMetaDaoSpec extends MetaDaoSpecBase {
  val file = File.createTempFile("test-meta", ".json")
  file.deleteOnExit()

  override val metaDao: MetaDao =
    new JsonMetaDao(file) with CachingMetaDao
}
