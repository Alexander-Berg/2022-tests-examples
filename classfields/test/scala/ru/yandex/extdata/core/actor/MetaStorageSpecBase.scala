package ru.yandex.extdata.core.actor

import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.extdata.core.{DataType, InstanceHeader}
import ru.yandex.extdata.core.gens.Generator
import ru.yandex.extdata.core.gens.Producer._
import ru.yandex.extdata.core.storage.{Meta, MetaStorage}

/**
  * @author evans
  */
trait MetaStorageSpecBase extends WordSpecLike with Matchers {

  def metaStorage: MetaStorage

  private def incVersion(meta: Meta) =
    meta.copy(
      instanceHeader = meta.instanceHeader.copy(version = meta.instanceHeader.version)
    )

  private def buildMeta(name: String, format: Int, version: Int) =
    Meta(DataType(name, format), InstanceHeader("", version, DateTime.now))

  "MetaStorage" should {
    "get last meta with old version" in {
      metaStorage.clear()
      val prevMeta = Generator.MetaGen.next
      val nextMeta = incVersion(prevMeta)
      metaStorage.save(prevMeta)
      metaStorage.save(nextMeta)
      val actualMeta =
        metaStorage.getIfModified(prevMeta.dataType, Some(prevMeta.instanceHeader.version - 1))
      actualMeta.get.get shouldEqual nextMeta
    }
    "get last meta without version" in {
      metaStorage.clear()
      val prevMeta = Generator.MetaGen.next
      val nextMeta = incVersion(prevMeta)
      metaStorage.save(prevMeta)
      metaStorage.save(nextMeta)
      val actualMeta =
        metaStorage.getIfModified(prevMeta.dataType, None)
      actualMeta.get.get shouldEqual nextMeta
    }
    "get last meta with current version" in {
      metaStorage.clear()
      val meta = Generator.MetaGen.next
      metaStorage.save(meta)
      val actualMeta =
        metaStorage.getIfModified(meta.dataType, Some(meta.instanceHeader.version))
      actualMeta.get shouldEqual None
    }

    "get all meta" in {
      metaStorage.clear()
      val meta = buildMeta("a", 1, 1)
      val meta2 = buildMeta("b", 1, 1)
      metaStorage.save(meta)
      metaStorage.save(meta2)
      metaStorage.get.get.toSet shouldEqual Seq(meta, meta2).toSet
    }

    "get meta by data type" in {
      metaStorage.clear()
      val meta = buildMeta("a", 1, 1)
      val meta2 = buildMeta("b", 1, 1)
      metaStorage.save(meta)
      metaStorage.save(meta2)
      metaStorage.get(DataType("a", 1)).get shouldEqual Seq(meta)
    }

    "get meta by data type string" in {
      metaStorage.clear()
      val meta = buildMeta("a", 1, 1)
      val meta2 = buildMeta("b", 1, 1)
      val meta3 = buildMeta("a", 2, 1)
      metaStorage.save(meta)
      metaStorage.save(meta2)
      metaStorage.save(meta3)
      metaStorage.get("a").get.toSet shouldEqual Seq(meta, meta3).toSet
    }

    "get meta by data type and version" in {
      metaStorage.clear()
      val meta = buildMeta("a", 1, 1)
      val meta2 = buildMeta("b", 1, 1)
      val meta3 = buildMeta("a", 2, 1)
      metaStorage.save(meta)
      metaStorage.save(meta2)
      metaStorage.save(meta3)
      metaStorage.get(DataType("a"), 1).get shouldEqual meta
    }

    "get meta by data type and version if not exists" in {
      metaStorage.clear()
      intercept[NoSuchElementException] {
        metaStorage.get(DataType("a"), 1).get
      }
    }

    "get last meta" in {
      metaStorage.clear()
      val meta = buildMeta("a", 1, 1)
      val meta2 = buildMeta("b", 1, 1)
      val meta3 = buildMeta("a", 1, 2)
      metaStorage.save(meta)
      metaStorage.save(meta2)
      metaStorage.save(meta3)
      metaStorage.getLast(DataType("a")).get shouldEqual Some(meta3)
    }

    "get last meta if not exists" in {
      metaStorage.clear()
      metaStorage.getLast(DataType("a")).get shouldEqual None
    }

    "delete meta" in {
      metaStorage.clear()
      val meta = buildMeta("a", 1, 1)
      metaStorage.save(meta)
      metaStorage.delete(meta)
      metaStorage.get.get shouldEqual Seq.empty
    }
  }
}
