package vertis.palma.dao.ydb.storage

import org.scalacheck.Gen
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.ydb.Ydb
import vertis.palma.dao.model.{Pagination, RelationNotFoundException}
import vertis.palma.dao.ydb.storage.PaginationTestHelper.readAll
import vertis.palma.gen.ModelGenerators._
import vertis.ydb.YEnv
import vertis.ydb.test.YdbTest
import vertis.zio.test.ZioSpecBase
import zio.{RIO, ZIO}

/** @author kusaeva
  */
class RelationsYdbStorageIntSpec extends ZioSpecBase with YdbTest {

  def ioTest(action: RelationsYdbStorage => RIO[YEnv, _]): Unit = {
    val storage = new RelationsYdbStorage(ydbWrapper, TestOperationalSupport.prometheusRegistry)
    zioRuntime.unsafeRunTask(action(storage))
    ()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    ioTest { dao =>
      dao.init()
    }
  }

  "RelationsYdbStorage" should {
    "create" in ioTest { storage =>
      val from = IdGen.next
      val relations = RelationGen.next(5)
      for {
        _ <- Ydb.runTx(storage.upsertBatch(from, relations))
        loaded <- ZIO.foreach(relations)(r => Ydb.runTx(storage.read(from, r)))
        _ <- check(loaded should contain theSameElementsAs relations)
      } yield ()
    }
    "delete" in {
      intercept[RelationNotFoundException] {
        ioTest { storage =>
          val from = IdGen.next
          val n = Gen.choose(5, 10).next
          val relations = RelationGen.next(n).toSeq
          for {
            _ <- Ydb.runTx(storage.upsertBatch(from, relations))
            _ <- Ydb.runTx(storage.deleteBatch(from, relations))
            results <- ZIO.foreach(relations)(r => Ydb.runTx(storage.read(from, r)).either)
            _ <- check("all deleted:")(results.count(_.isLeft) shouldBe n)
          } yield results.head match {
            case Left(e) => throw e
            case Right(_) => ()
          }
        }
      }
    }
    "find related" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val from = IdGen.next.copy(dictionaryId = dictionaryId)
      val relation = RelationGen.next

      for {
        _ <- Ydb.runTx(storage.upsertBatch(from, Seq(relation)))
        loaded <- Ydb.runTx(storage.findRelated(relation.to))
        _ <- check(loaded.head shouldBe from)
      } yield ()
    }
    "list related with pagination" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val fromIds = IdGen.next(10).map(_.copy(dictionaryId = dictionaryId))
      val relation = RelationGen.next
      val pagination = Pagination(None, 10)

      val readPage =
        (p: Pagination) => Ydb.runTx(storage.listRelated(relation.to, dictionaryId, relation.field, p))

      for {
        _ <- ZIO.foreach_(fromIds)(from => Ydb.runTx(storage.upsertBatch(from, Seq(relation))))
        loaded <- readAll(Nil, pagination, readPage)
        _ <- check(loaded should contain theSameElementsAs fromIds)
      } yield ()
    }
    "return empty list when no related" in ioTest { storage =>
      val dictionaryId = StrGen.next
      val field = StrGen.next
      val to = IdGen.next
      for {
        related <- Ydb.runTx(storage.listRelated(to, dictionaryId, field, Pagination.default))
        _ <- check(related.items shouldBe empty)
      } yield ()
    }
  }

}
