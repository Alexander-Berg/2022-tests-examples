package ru.yandex.vertis.general.bonsai.logic.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.bonsai.attribute_model.AttributeDefinition
import general.bonsai.category_model.{Category, CategoryState}
import general.bonsai.internal.internal_api.{GlobalHistoryFilter, PagingRequest}
import ru.yandex.vertis.general.bonsai.logic.GlobalHistoryManager
import ru.yandex.vertis.general.bonsai.model.BonsaiEntity._
import ru.yandex.vertis.general.bonsai.model.CommentType.Comment
import ru.yandex.vertis.general.bonsai.model.{BonsaiEntity, EntityRef, HistoryRecord, Latest}
import ru.yandex.vertis.general.bonsai.storage.testkit._
import ru.yandex.vertis.general.bonsai.storage.ydb.sign.EntityUpdateChecker
import ru.yandex.vertis.general.bonsai.storage.ydb.{BonsaiTxRunner, YdbEntityDao, YdbGlobalHistoryDao, YdbHistoryDao}
import ru.yandex.vertis.general.bonsai.storage.{EntityDao, GlobalHistoryDao, HistoryDao}
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object DefaultGlobalHistoryManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultGlobalHistoryManager")(
      testM("find record") {
        for {
          _ <- createEntity(v => Category(name = "testName", version = v, id = "test"))
          res <- GlobalHistoryManager.filter(GlobalHistoryFilter(None, None), PagingRequest("", 10))
        } yield assert(res.records.nonEmpty)(isTrue)
      },
      testM("load previous version") {
        for {
          _ <- createEntity(v => Category(id = "cat1", version = v))
          _ <- updateEntity[Category]("cat1", v => _.copy(name = "hello", version = v))
          _ <- updateEntity[Category]("cat1", v => _.copy(state = CategoryState.FORBIDDEN, version = v))
          res <- GlobalHistoryManager.filter(GlobalHistoryFilter(None, None), PagingRequest("", 2))
        } yield assert(res.records.forall(_.previousVersion.nonEmpty))(isTrue)
      },
      testM("support filters and paging") {
        for {
          _ <- ZIO.foreach(1 to 30)(i => createEntity(v => Category(id = "filter_cat" + i, version = v)))
          _ <- ZIO.foreach(1 to 30)(i => createEntity(v => AttributeDefinition(id = "attr" + i, version = v)))
          res1 <- GlobalHistoryManager.filter(GlobalHistoryFilter(Some(5), Some(45)), PagingRequest("", 5))
          res2 <- GlobalHistoryManager.filter(
            GlobalHistoryFilter(Some(5), Some(45)),
            PagingRequest(res1.paging.get.cursorEnd, 36)
          )
          res3 <- GlobalHistoryManager.filter(
            GlobalHistoryFilter(Some(5), Some(45)),
            PagingRequest(res2.paging.get.cursorEnd, 10)
          )
        } yield {
          val sizeCorrect = res1.records.lengthCompare(5) == 0
          val directionCorrect = res2.records.forall(r => res1.records.forall(_.version >= r.version))
          val stop = res3.paging.get.cursorEnd.isEmpty
          assert(sizeCorrect)(isTrue) &&
          assert(directionCorrect)(isTrue) &&
          assert(stop)(isTrue)
        }
      },
      testM("keep ordering on batch inserts") {
        for {
          now <- zio.clock.instant
          inserted <- runTx {
            for {
              v <- HistoryDao.newVersion(now, 0, Comment("comment"))
              records = (1 to 40).map { i =>
                val id = "batch_cat_" + i
                HistoryRecord(v, None, EntityRef(BonsaiEntity[Category]._type, id))
              }.toList
              _ <- GlobalHistoryDao.createRecords(records)
            } yield records
          }
          res <- GlobalHistoryManager.filter(GlobalHistoryFilter(None, None), PagingRequest("", 20))
          res2 <-
            GlobalHistoryManager.filter(GlobalHistoryFilter(None, None), PagingRequest(res.paging.get.cursorEnd, 20))
        } yield {
          val all = res.records.toList ::: res2.records.toList
          val stayOrdered = all.map(_.id) == inserted.reverse.map(_.ref.id)
          val sameVersion = all.groupBy(_.version).size == 1
          assert(stayOrdered)(isTrue) &&
          assert(sameVersion)(isTrue)
        }
      }
    )
  }.provideCustomLayer {
    val clock = Clock.live
    val ydb = TestYdb.ydb
    val updateChecker = EntityUpdateChecker.live
    val txRunner = ((ydb >>> Ydb.txRunner) ++ updateChecker) >>> BonsaiTxRunner.live
    val historyDao = ydb >>> YdbHistoryDao.live
    val globalHistoryDao = ydb >>> YdbGlobalHistoryDao.live
    val entityDao = (ydb ++ updateChecker) >>> YdbEntityDao.live

    val deps = historyDao ++ entityDao ++ globalHistoryDao ++ txRunner ++ clock
    (deps >>> GlobalHistoryManager.live) ++ historyDao ++ globalHistoryDao ++ txRunner ++ entityDao
  }

  private def createEntity[T: BonsaiEntity](build: Long => T) = {
    zio.clock.instant.flatMap { now =>
      runTx {
        for {
          v <- HistoryDao.newVersion(now, 0, Comment("comment"))
          entity = build(v)
          _ <- EntityDao.createEntity(entity)
          rec = HistoryRecord(v, None, EntityRef(BonsaiEntity[T]._type, entity.id))
          _ <- GlobalHistoryDao.createRecords(rec :: Nil)
        } yield v
      }
    }
  }

  private def updateEntity[T: BonsaiEntity](id: String, update: Long => T => T) = {
    zio.clock.instant.flatMap { now =>
      runTx {
        for {
          v <- HistoryDao.newVersion(now, 0, Comment("comment"))
          old <- EntityDao.getEntity[T](id, Latest)
          _ <- EntityDao.updateEntity(update(v)(old))
          rec = HistoryRecord(v, Some(old.version), EntityRef(BonsaiEntity[T]._type, id))
          _ <- GlobalHistoryDao.createRecords(rec :: Nil)
        } yield v
      }
    }
  }
}
