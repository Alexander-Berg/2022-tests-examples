package ru.yandex.extdata.core.actor.impl

import java.io.File

import ru.yandex.extdata.core.actor.MetaStorageSpecBase
import ru.yandex.extdata.core.storage.MetaStorage
import ru.yandex.extdata.core.storage.impl.{JsonMetaDao, MetaStorageImpl}

/**
  * @author evans
  */
class MetaStorageImplSpec extends MetaStorageSpecBase {
  val file = File.createTempFile("test-meta", ".json")
  file.deleteOnExit()

  override def metaStorage: MetaStorage = new MetaStorageImpl(new JsonMetaDao(file))
}
