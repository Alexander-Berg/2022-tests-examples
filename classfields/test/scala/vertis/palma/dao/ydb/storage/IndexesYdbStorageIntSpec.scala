package vertis.palma.dao.ydb.storage

import org.scalacheck.Gen
import ru.yandex.vertis.ydb.Ydb
import vertis.palma.dao.model.{IndexNotFoundException, Pagination}
import vertis.palma.dao.ydb.storage.PaginationTestHelper.readAll
import vertis.palma.gen.ModelGenerators._
import vertis.ydb.YEnv
import vertis.ydb.test.YdbTest
import vertis.zio.test.ZioSpecBase
import zio.{RIO, ZIO}

/** @author kusaeva
  */
class IndexesYdbStorageIntSpec extends ZioSpecBase with YdbTest {

  private lazy val dictionaries = new DictionariesYdbStorage(ydbWrapper, prometheusRegistry)
  private lazy val indexes = new IndexesYdbStorage(ydbWrapper, prometheusRegistry)

  def ioTest(action: IndexesYdbStorage => RIO[YEnv, _]): Unit = {
    zioRuntime.unsafeRunTask(action(indexes))
    ()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    ioTest { indexes =>
      indexes.init() *>
        dictionaries.init()
    }
  }

  "IndexesYdbStorage" should {
    "create" in ioTest { storage =>
      val id = IdGen.next
      val indexes = IndexGen.next(5)
      for {
        _ <- Ydb.runTx(storage.upsertBatch(id, indexes))
        loaded <- ZIO.foreach(indexes)(r => Ydb.runTx(storage.read(id, r)))
        _ <- check(loaded should contain theSameElementsAs indexes)
      } yield ()
    }
    "delete" in {
      intercept[IndexNotFoundException] {
        ioTest { storage =>
          val id = IdGen.next
          val n = Gen.choose(3, 6).next
          val indexes = IndexGen.next(n).toSeq
          for {
            _ <- Ydb.runTx(storage.upsertBatch(id, indexes))
            _ <- Ydb.runTx(storage.deleteBatch(id, indexes))
            results <- ZIO.foreach(indexes)(i => Ydb.runTx(storage.read(id, i)).either)
            _ <- check("all deleted:")(results.count(_.isLeft) shouldBe n)
          } yield results.head match {
            case Left(e) => throw e
            case Right(_) => ()
          }
        }
      }
    }
    "list items by index with pagination" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val fromIds = IdGen.next(10).map(_.copy(dictionaryId = dictionaryId))
      val index = IndexGen.next
      val pagination = Pagination(None, 10)

      val readPage =
        (p: Pagination) => Ydb.runTx(storage.list(dictionaryId, index, p))

      for {
        _ <- ZIO.foreach(fromIds)(from => Ydb.runTx(storage.upsertBatch(from, Seq(index))))
        loaded <- readAll(Nil, pagination, readPage)
        _ <- check(loaded should contain theSameElementsAs fromIds)
      } yield ()
    }
  }
}
