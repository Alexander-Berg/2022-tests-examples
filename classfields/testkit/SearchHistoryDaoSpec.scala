package ru.yandex.vertis.general.personal.storage.testkit

import common.zio.ydb.Ydb.HasTxRunner
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.personal.model.UserSearchHistory
import ru.yandex.vertis.general.personal.model.testkit.SearchHistoryEntryGen
import ru.yandex.vertis.general.personal.storage.SearchHistoryDao
import ru.yandex.vertis.general.personal.storage.SearchHistoryDao.SearchHistoryDao
import zio.clock.Clock
import zio.random.{nextIntBounded, Random}
import zio.test.Assertion._
import zio.test.{Spec, _}

import java.time.temporal.ChronoUnit

object SearchHistoryDaoSpec {

  def spec(
      label: String): Spec[SearchHistoryDao with Clock with HasTxRunner with Random with Sized with TestConfig, TestFailure[Nothing], TestSuccess] = {
    suite(label)(
      testM("save search") {
        checkNM(1)(SearchHistoryEntryGen.anySearchHistoryEntry.noShrink, OwnerIdGen.anyOwnerId) { (entry, owner) =>
          for {
            _ <- runTx(SearchHistoryDao.saveHistory(owner, UserSearchHistory(List(entry))))
            saved <- runTx(SearchHistoryDao.getHistory(owner))
          } yield assert(saved.flatMap(_.entries.headOption))(isSome(equalTo(entry)))
        }
      },
      testM("get searches ordered by timestamp") {
        checkNM(1)(
          Gen.listOfN(20)(SearchHistoryEntryGen.anySearchHistoryEntry.noShrink),
          OwnerIdGen.anyOwnerId.noShrink
        ) { (records, owner) =>
          for {
            _ <- runTx(SearchHistoryDao.updateHistory(owner, records))
            history <- runTx(SearchHistoryDao.getHistory(owner))
          } yield assert(history)(isSome(equalTo(UserSearchHistory(records.sortBy(_.timestamp).reverse))))
        }
      },
      testM("keep only unique searches") {
        checkNM(1)(
          Gen.listOfN(20)(SearchHistoryEntryGen.anySearchHistoryEntry.noShrink),
          OwnerIdGen.anyOwnerId.noShrink
        ) { (records, owner) =>
          val (batch1, batch2) = records.splitAt(10)
          val maxTime1 = batch1.map(_.timestamp).max
          val withDuplicateText = batch1.zip(batch2).map { case (r1, r2) =>
            r2.copy(text = r1.text, timestamp = maxTime1.plus(1, ChronoUnit.HOURS))
          }

          for {
            _ <- runTx(SearchHistoryDao.updateHistory(owner, batch1))
            _ <- runTx(SearchHistoryDao.updateHistory(owner, withDuplicateText))
            history <- runTx(SearchHistoryDao.getHistory(owner))
          } yield assert(history.map(_.entries))(
            isSome(equalTo(withDuplicateText))
          )
        }
      },
      testM("clear search request from history") {
        checkNM(1)(
          Gen.listOfN(20)(SearchHistoryEntryGen.anySearchHistoryEntry.noShrink),
          OwnerIdGen.anyOwnerId.noShrink
        ) { (records, owner) =>
          for {
            _ <- runTx(SearchHistoryDao.updateHistory(owner, records))
            randomIndex <- nextIntBounded(records.size)
            recordToRemove = records(randomIndex)
            _ <- runTx(SearchHistoryDao.clearHistoryEntry(owner, recordToRemove.text))
            history <- runTx(SearchHistoryDao.getHistory(owner))
            expectedRecords = (records.take(randomIndex) ++ records.drop(randomIndex + 1)).sortBy(_.timestamp).reverse
          } yield assert(history)(isSome(equalTo(UserSearchHistory(expectedRecords))))
        }
      },
      testM("clear all history") {
        checkNM(1)(
          Gen.listOfN(20)(SearchHistoryEntryGen.anySearchHistoryEntry.noShrink),
          OwnerIdGen.anyOwnerId.noShrink
        ) { (records, owner) =>
          for {
            _ <- runTx(SearchHistoryDao.updateHistory(owner, records))
            _ <- runTx(SearchHistoryDao.clearHistory(owner))
            history <- runTx(SearchHistoryDao.getHistory(owner))
          } yield assert(history)(isSome(hasField("entries", _.entries, hasSize(equalTo(0)))))
        }
      }
    )
  }
}
