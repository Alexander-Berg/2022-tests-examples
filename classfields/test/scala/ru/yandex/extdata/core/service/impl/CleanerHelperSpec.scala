package ru.yandex.extdata.core.service.impl

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.extdata.core.DataType
import ru.yandex.extdata.core.gens.Generator
import ru.yandex.extdata.core.gens.Producer._
import ru.yandex.extdata.core.spec.impl.ProducerSpec
import ru.yandex.extdata.core.storage.Meta

import scala.concurrent.duration._

/**
  * @author evans
  */
class CleanerHelperSpec extends WordSpecLike with Matchers {
  private val delay = 1.day

  def withDecVersionAndTime(meta: Meta): Meta =
    meta.copy(
      instanceHeader = meta.instanceHeader.copy(
        version = meta.instanceHeader.version - 1,
        produceTime = meta.instanceHeader.produceTime.minus(delay.toMillis + 1)
      )
    )

  def withDecVersion(meta: Meta): Meta =
    meta.copy(instanceHeader = meta.instanceHeader.copy(version = meta.instanceHeader.version - 1))

  def withDecFormat(meta: Meta): Meta =
    meta.copy(dataType = meta.dataType.copy(format = meta.dataType.format - 1))

  def dataTypeSpec(dataType: DataType, keepVersion: Int, cleanDelay: FiniteDuration) =
    ProducerSpec(dataType = dataType, weight = 1, keepVersions = keepVersion, cleanDelay = delay)

  "Cleaner helper" should {
    "nothing do for valid metas" in {
      val meta = Generator.MetaGen.next
      val spec = dataTypeSpec(meta.dataType, 10, delay)
      val obsolete = new CleanerHelper(3, Map(meta.dataType -> spec)).findObsolete(Seq(meta))
      obsolete.isEmpty shouldEqual true
    }
    "delete obsolete versions of actual data type" in {
      val meta = Generator.MetaGen.next
      val meta2 = withDecVersionAndTime(meta)
      val meta3 = withDecVersionAndTime(meta2)
      val spec = dataTypeSpec(meta.dataType, 2, delay)
      val obsolete = new CleanerHelper(3, Map(meta.dataType -> spec))
        .findObsolete(Seq(meta, meta2, meta3))
      obsolete shouldEqual Seq(meta3)
    }
    "delete obsolete versions of oldest produce time" in {
      val meta = Generator.MetaGen.next
      val meta2 = withDecVersion(meta)
      val meta3 = withDecVersionAndTime(meta2)
      val meta4 = withDecVersionAndTime(meta3)
      val spec = dataTypeSpec(meta.dataType, 1, delay)
      val obsolete = new CleanerHelper(3, Map(meta.dataType -> spec))
        .findObsolete(Seq(meta, meta2, meta3, meta4))
      obsolete shouldEqual Seq(meta3, meta4)
    }
    "delete unknown data types" in {
      val meta = Generator.MetaGen.next
      val obsolete = new CleanerHelper(3, Map.empty)
        .findObsolete(Seq(meta))
      obsolete shouldEqual Seq(meta)
    }
    "delete obsolete format" in {
      val meta = Generator.MetaGen.next
      val meta2 = withDecVersionAndTime(meta)
      val meta3 = withDecVersionAndTime(meta2)
      val spec = dataTypeSpec(meta.dataType, 2, delay)
      val obsolete = new CleanerHelper(2, Map(meta.dataType -> spec))
        .findObsolete(Seq(meta, meta2, meta3))
      obsolete shouldEqual Seq(meta3)
    }
    "delete obsolete versions for valid old format" in {
      val meta = Generator.MetaGen.next
      val meta2 = withDecFormat(meta)
      val meta3 = withDecVersionAndTime(meta2)
      val meta4 = withDecVersionAndTime(meta3)
      val spec = dataTypeSpec(meta.dataType, 2, delay)
      val obsolete = new CleanerHelper(2, Map(meta.dataType -> spec))
        .findObsolete(Seq(meta, meta2, meta3, meta4))
      obsolete shouldEqual Seq(meta4)
    }
    "never delete current format" in {
      val meta = Generator.MetaGen.next
      val meta2 = withDecVersionAndTime(meta)
      val spec = dataTypeSpec(meta.dataType, 2, delay)
      val obsolete = new CleanerHelper(1, Map(meta2.dataType -> spec))
        .findObsolete(Seq(meta, meta2))
      obsolete shouldEqual Seq.empty
    }
    "keep obsolete versions for time (in case somebody uses this)" in {
      val meta = Generator.MetaGen.next
      val meta2 = withDecVersion(meta)
      val meta3 = withDecVersion(meta2)
      val meta4 = withDecVersion(meta3)
      val spec = dataTypeSpec(meta.dataType, 2, delay)
      val obsolete = new CleanerHelper(1, Map(meta2.dataType -> spec))
        .findObsolete(Seq(meta, meta2, meta3, meta4))
      obsolete shouldEqual Seq.empty
    }
  }
}
