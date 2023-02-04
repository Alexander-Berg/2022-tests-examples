package ru.yandex.vertis.picapica.dao.impl

import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import ru.yandex.vertis.picapica.dao.StorageDao
import ru.yandex.vertis.picapica.generators.Generators._
import ru.yandex.vertis.picapica.generators.Producer._
import ru.yandex.vertis.picapica.model.AvatarsResponse.AvatarsData
import ru.yandex.vertis.picapica.model.{Id, Service, StoredAvatarsData}

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable}

/**
  * @author evans
  */
trait StorageDaoSpec
    extends WordSpec
        with Matchers
        with BeforeAndAfterEach {

  def idToUrlPart(id: Id): String

  def dao: StorageDao

  val avatarsId: AvatarsData = AvatarsData(1, "foobar", None)

  def result[T](awaitable: Awaitable[T]): T =
    Await.result(awaitable, 20.seconds)

  override def beforeEach(): Unit = {
    super.beforeEach()
    clear()
  }

  protected def clear(): Unit

  "batched CassandraStorageDAO" should {
    val size = 10
    val values = (IdGen.next(size) zip ActualIdGen.next(size)).toMap
    val partitionId = 3

    "store elements" in {
      result(dao.store(values)(partitionId))
    }

    "get batch" in {
      val tasks: Iterable[Id] = values.keys.toVector
      result(dao.store(values)(partitionId))
      result(dao.get(partitionId, tasks)).map(o => (o._1, o._2.id)).toSet shouldEqual values.toSet
    }
  }
  "CassandraStorageDAO" should {
    val size = 5
    val values = (IdGen.next(size) zip ActualIdGen.next(size)).toMap
    val partitionId = 3

    "store elements" in {
      result(dao.store(values)(partitionId))
    }

    "set name from task if name is NULL" in {
      val avatarsData = AvatarsData(7, null, None) // scalastyle:ignore
      val task = TaskGen.next
      result(dao.store(task.id, avatarsData)(partitionId))
      val stored = result(dao.get(partitionId, Vector(task.id))).head._2
      stored.id shouldEqual avatarsData.copy(name = idToUrlPart(task.id))
    }

    "not store expired ids" in {
      val id = IdGen.next
      val actualId = ActualIdGen.next.copy(expiresAt = Some(DateTime.now().minusSeconds(1)))
      result(dao.store(id, actualId)(partitionId))
      result(dao.get(partitionId, Vector(id))) should be (empty)
    }

    "apply ttl correctly" in {
      val id = IdGen.next
      val expiresAt = DateTime.now().plusSeconds(3)
      val actualId = ActualIdGen.next.copy(expiresAt = Some(expiresAt))
      result(dao.store(id, actualId)(partitionId))
      result(dao.get(partitionId, Vector(id))) shouldNot be (empty)
      Thread.sleep(5000)
      result(dao.get(partitionId, Vector(id))) should be (empty)
    }

    "get by elem" in {
      val tasks: Iterable[Id] = values.keys.toVector
      result(dao.store(values)(partitionId))
      val got = result(dao.get(partitionId, tasks)).map(o => (o._1, o._2.id.id))
      got.toSet shouldEqual values.mapValues(_.id).toSet
    }

    "get meta" in {
      val id = IdGen.next
      val meta = MetaMapGen.next
      result(dao.store(id, avatarsId)(partitionId))
      result(dao.storeMeta(id, meta, None)(partitionId))
      result(dao.get(partitionId, Vector(id))).head._2.meta shouldEqual meta
    }

    "get meta in binary form" in {
      val id = IdGen.next
      val meta = MetaMapGen.next
      val metadata = MetadataGen.next
      result(dao.store(id, avatarsId)(partitionId))
      result(dao.storeMeta(id, meta, Some(metadata))(partitionId))
      val stored = result(dao.get(partitionId, Vector(id))).head._2
      stored.meta shouldEqual meta
      stored.metadata.get shouldEqual metadata
    }

    "get both meta and data" in {
      val id = IdGen.next
      val meta = MetaMapGen.next
      result(dao.store(id, avatarsId)(partitionId))
      result(dao.storeMeta(id, meta, None)(partitionId))
      result(dao.get(partitionId, Vector(id)))
          .head._2 shouldEqual StoredAvatarsData(avatarsId, meta, None)
    }

    "can store meta without binary part" in {
      val id = IdGen.next
      val meta = MetaMapGen.next
      result(dao.store(id, avatarsId)(partitionId))
      result(dao.storeMeta(id, meta, None)(partitionId))
      val stored = result(dao.get(partitionId, Vector(id))).head._2
      stored.meta shouldEqual meta
      stored.metadata should be(empty)
    }

  }
}
