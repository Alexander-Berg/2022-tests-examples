package ru.yandex.extdata.core.actor.impl

import java.io.File

import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import ru.yandex.extdata.core.actor.{MetaDaoSpecBase, MetaStorageSpecBase}
import ru.yandex.extdata.core.storage.impl.JsonMetaDao
import ru.yandex.extdata.core.storage.{Meta, MetaDao, MetaStorage}
import ru.yandex.extdata.core.{DataType, InstanceHeader}

/**
  * @author evans
  */
class JsonMetaDaoSpec extends MetaDaoSpecBase {
  val file = File.createTempFile("test-meta", ".json")
  file.deleteOnExit()

  override val metaDao: MetaDao = new JsonMetaDao(file)

  "File json meta storage" should {
    "work without file" in {
      file.delete()
      new JsonMetaDao(file).get().isEmpty shouldEqual true
    }
  }
}
