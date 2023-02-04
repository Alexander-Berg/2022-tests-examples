package vertis.palma.dao.ydb.storage

import ru.yandex.vertis.palma.service_common.Sorting.Direction.DESCENDING
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import ru.yandex.vertis.ydb.Ydb
import vertis.palma.dao.model.DictionaryItem.{Id, PlainPayload}
import vertis.palma.dao.model.Sorting.{FieldTypes, JsonField}
import vertis.palma.dao.model.{DictionaryItem, Index, ItemNotFoundException, Pagination, Sorting}
import vertis.palma.dao.ydb.YdbColumns
import vertis.palma.dao.ydb.storage.PaginationTestHelper.readAll
import vertis.palma.gen.ModelGenerators._
import vertis.palma.gen.ModelProtoGenerators.markGen
import vertis.palma.model.CustomEquality.DictionaryItemEquality
import vertis.palma.test.SortModel
import vertis.palma.utils.ProtoUtils.Json
import vertis.ydb.YEnv
import vertis.ydb.test.YdbTest
import vertis.zio.test.ZioSpecBase
import zio.{RIO, ZIO}

import scala.util.Random

/** @author kusaeva
  */
class DictionariesYdbStorageIntSpec extends ZioSpecBase with YdbTest {

  def ioTest(action: DictionariesYdbStorage => RIO[YEnv, _]): Unit = {
    val storage = new DictionariesYdbStorage(ydbWrapper, prometheusRegistry)
    zioRuntime.unsafeRunTask(action(storage))
    ()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    ioTest { dao =>
      dao.init()
    }
  }

  private def createPayload(m: SortModel): PlainPayload = {
    val mark = markGen.next
    val msg = m.toBuilder
      .setCode(StrGen.next)
      .setMark(mark)
      .build
    val jsonView = zioRuntime.unsafeRunTask(Json.printInternal(msg))
    DictionaryItem.PlainPayload(msg.toByteArray, jsonView)
  }

  private def addItems(storage: DictionariesYdbStorage, dictionaryId: String, count: Int): RIO[YEnv, Seq[Id]] = {
    val items = DictionaryItemGen.next(count).map { item =>
      val msg =
        RandomProtobufGenerator
          .genForAuto[SortModel]
          .next
          .toBuilder
          .setRussianAlias(StrGen.next)
          .build()
      (
        item.copy(
          id = item.id.copy(dictionaryId = dictionaryId),
          payload = createPayload(msg)
        ),
        msg.getRussianAlias
      )
    }
    val expected =
      items.toSeq
        .sortBy { case (_, s) => s }
        .map { case (item, _) => item.id }

    ZIO
      .foreach_(items) { case (item, _) =>
        Ydb.runTx(storage.create(item))
      }
      .as(expected)
  }

  private def addItemsWithIndex[A: Ordering](
      storage: DictionariesYdbStorage,
      dictionaryId: String,
      count: Int,
      getSortField: SortModel => A,
      index: Index): RIO[YEnv, Seq[(Id, String)]] = {

    val items = DictionaryItemGen.next(count).map { item =>
      val m = RandomProtobufGenerator.genForAuto[SortModel].next
      val msg =
        if (Random.nextBoolean()) {
          m.toBuilder
            .setAlias(index.value)
            .build()
        } else {
          m
        }
      (
        item.copy(
          id = item.id.copy(dictionaryId = dictionaryId),
          payload = createPayload(msg)
        ),
        getSortField(msg),
        msg.getAlias
      )
    }
    val expected =
      items.toSeq
        .filter { case (_, _, a) => a == index.value }
        .sortBy { case (_, s, _) => s }
        .map { case (item, _, _) => (item.id, item.payload.asInstanceOf[PlainPayload].jsonView) }

    ZIO
      .foreach_(items) { case (item, _, _) =>
        Ydb.runTx(storage.create(item))
      }
      .as(expected)
  }

  "DictionariesYdbStorage" should {
    "create and read item" in ioTest { storage =>
      val item = DictionaryItemGen.next
      for {
        _ <- Ydb.runTx(storage.create(item))
        loaded <- Ydb.runTx(storage.read(item.id))
        _ <- check(loaded should equal(item))
      } yield ()
    }
    "read batch" in ioTest { storage =>
      val items = DictionaryItemGen.next(10).map(_.copy(relations = Nil))
      val other = DictionaryItemGen.next(5)
      for {
        _ <- ZIO.foreach(items)(item => Ydb.runTx(storage.create(item)))
        loaded <- Ydb.runTx(storage.readBatch((items ++ other).map(_.id)))
        notExisting <- Ydb.runTx(storage.readBatch(other.map(_.id)))
        _ <- check("loaded:")(loaded should contain theSameElementsAs items)
        _ <- check("not existing:")(notExisting shouldBe empty)
      } yield ()
    }
    "check batch for existence" in ioTest { storage =>
      val items = DictionaryItemGen.next(10).map(_.copy(relations = Nil))
      val other = DictionaryItemGen.next(5)
      for {
        _ <- ZIO.foreach(items)(item => Ydb.runTx(storage.create(item)))
        exists <- Ydb.runTx(storage.existsBatch(items.map(_.id).toSet))
        notExists <- Ydb.runTx(storage.existsBatch(other.map(_.id).toSet))
        _ <- check("exists:")(exists shouldBe true)
        _ <- check("not exists:")(notExists shouldBe false)

      } yield ()
    }
    "delete item" in {
      intercept[ItemNotFoundException] {
        ioTest { storage =>
          val item = DictionaryItemGen.next
          for {
            _ <- Ydb.runTx(storage.create(item))
            _ <- Ydb.runTx(storage.delete(item.id))
            _ <- Ydb.runTx(storage.read(item.id))
          } yield ()
        }
      }
    }
    "list items with pagination" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val items = DictionaryItemGen
        .next(100)
        .map(i => i.copy(id = i.id.copy(dictionaryId = dictionaryId)))
      val limit = 10
      val pagination = Pagination(limit = limit)

      val readPage = (p: Pagination) => Ydb.runTx(storage.list(dictionaryId, p))

      for {
        _ <- ZIO.foreach(items)(item => Ydb.runTx(storage.create(item)))
        loaded <- readAll(Nil, pagination, readPage)
        _ <- check(loaded should contain theSameElementsAs items)
      } yield ()
    }
    "return empty list when no items" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val pagination = Pagination(limit = 100)
      for {
        page <- Ydb.runTx(storage.list(dictionaryId, pagination))
        _ <- check(page.items shouldBe empty)
      } yield ()
    }

    "store and read item with encryption info" in ioTest { storage =>
      val encryptedPayload = EncryptedPayloadGen.next
      val items =
        DictionaryItemGen.next(5).map(_.copy(payload = encryptedPayload))

      for {
        _ <- ZIO.foreach(items)(item => Ydb.runTx(storage.create(item)))
        exists <- Ydb.runTx(storage.existsBatch(items.map(_.id).toSet))
        _ <- check("exists:")(exists shouldBe true)
      } yield ()
    }

    "sort items with pagination" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val count = 16
      val pagination = Pagination(None, 3)
      val readPage = (p: Pagination) =>
        storage.list(
          dictionaryId,
          p,
          None,
          Sorting(JsonField("russian_alias", FieldTypes.String, YdbColumns.JsonContentView))
        )

      for {
        expected <- addItems(storage, dictionaryId, count)
        loaded <- readAll(Nil, pagination, readPage).map(_.map(_.id))
        _ <- check {
          (loaded should contain).theSameElementsInOrderAs(expected)
        }
      } yield ()
    }
    "sort items with pagination and descending order" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val count = 3
      val pagination = Pagination(None, 3)

      val readPage = (p: Pagination) =>
        storage.list(
          dictionaryId,
          p,
          None,
          Sorting(JsonField("russian_alias", FieldTypes.String, YdbColumns.JsonContentView), DESCENDING)
        )

      for {
        expected <- addItems(storage, dictionaryId, count)
        loaded <- readAll(Nil, pagination, readPage).map(_.map(_.id))
        _ <- check {
          (loaded should contain).theSameElementsInOrderAs(expected.reverse)
        }
      } yield ()
    }
    "sort items with pagination and filter" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val count = 16
      val pagination = Pagination(None, 3)

      val index = Index("alias", "modelX")

      val readPage = (p: Pagination) =>
        storage.list(
          dictionaryId,
          p,
          Some(index),
          Sorting(JsonField("release_year", FieldTypes.Int32, YdbColumns.JsonContentView))
        )

      for {
        expected <- addItemsWithIndex(storage, dictionaryId, count, _.getReleaseYear, index)
        loaded <- readAll(Nil, pagination, readPage).map(
          _.map(x => (x.id, x.payload.asInstanceOf[PlainPayload].jsonView))
        )
        _ <- check {
          (loaded should contain).theSameElementsInOrderAs(expected)
        }
      } yield ()
    }
    "return correct last key when page size < pagination.limit" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val count = 3
      val pagination = Pagination(None, 10)
      val readPage = (p: Pagination) =>
        storage.list(
          dictionaryId,
          p,
          None,
          Sorting(JsonField("russian_alias", FieldTypes.String, YdbColumns.JsonContentView))
        )

      for {
        _ <- addItems(storage, dictionaryId, count)
        page <- readPage(pagination)
        _ <- check {
          (page.lastKey.get shouldBe count.toString)
        }
      } yield ()
    }
  }
}
