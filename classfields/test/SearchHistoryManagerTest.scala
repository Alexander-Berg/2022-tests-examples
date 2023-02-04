package ru.yandex.vertis.general.personal.logic.test

import common.clients.bigb.testkit.TestBigBrotherClient
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.proto.crypta.user_profile.Profile
import ru.yandex.proto.crypta.user_profile.Profile.Counter
import ru.yandex.vertis.general.bonsai.utils.CategoryHashUtil._
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.personal.logic.DefaultBigbProfileExtractor._
import ru.yandex.vertis.general.personal.logic.{BigbProfileExtractor, PersonalBonsaiSnapshot, SearchHistoryManager}
import ru.yandex.vertis.general.personal.model.testkit.SearchHistoryEntryGen
import ru.yandex.vertis.general.personal.model.{ClearAll, SearchHistoryEntry, UserSearchHistory}
import ru.yandex.vertis.general.personal.storage.SearchHistoryDao
import ru.yandex.vertis.general.personal.storage.ydb.YdbSearchHistoryDao
import ru.yandex.vertis.general.personal.testkit.TestPersonalBonsaiSnapshot
import ru.yandex.vertis.general.personal.testkit.TestPersonalBonsaiSnapshot._
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.{Gen, _}
import zio.{Has, Ref, ULayer}

import java.time.Instant

object SearchHistoryManagerTest extends DefaultRunnableSpec {

  private val profile = Profile(counters =
    Seq(
      Counter(
        counterId = Some(ColdStartCategoriesTimedCounterId.counterId),
        key = Seq(
          getCategoryIdHash(rootCategory1.id),
          getCategoryIdHash(rootCategory2.id)
        ),
        value = Seq(0, 0)
      ),
      Counter(
        counterId = Some(ColdStartCategoriesTimedCounterId.counterTimeId),
        key = Seq(
          getCategoryIdHash(rootCategory1.id),
          getCategoryIdHash(rootCategory2.id)
        ),
        value = Seq(1.0, 2.0)
      )
    )
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = ({
    suite("SearchHistoryManagerTest")(
      testM("Отдавать категории холодного старта если история пуста") {
        checkNM(1)(OwnerIdGen.anyOwnerId) { owner =>
          for {
            _ <- TestBigBrotherClient.setMapping(Map.empty.withDefaultValue(profile))
            (_, suggests) <- SearchHistoryManager.getSearchHistory(owner, 10)
          } yield assert(suggests.map(_.text))(
            hasSameElements(List(rootCategory1.name.toLowerCase, rootCategory2.name.toLowerCase))
          )
        }
      },
      testM("Подмешивать категории холодного старта если истории мало") {
        checkNM(1)(OwnerIdGen.anyOwnerId) { owner =>
          val historyText = "продать мопед"
          for {
            _ <- TestBigBrotherClient.setMapping(Map.empty.withDefaultValue(profile))
            _ <- Ydb.runTx(
              SearchHistoryDao.saveHistory(
                owner,
                UserSearchHistory(
                  SearchHistoryEntry(historyText, Instant.ofEpochSecond(1)) ::
                    SearchHistoryEntry(rootCategory2.name.toLowerCase, Instant.ofEpochMilli(0)) ::
                    Nil
                )
              )
            )
            (searches, suggests) <- SearchHistoryManager.getSearchHistory(owner, 5)
          } yield assert(searches.map(_.text))(
            hasSameElements(
              List(historyText, rootCategory2.name.toLowerCase)
            )
          ) && assert(suggests.map(_.text))(
            hasSameElements(
              List(rootCategory1.name.toLowerCase)
            )
          )
        }
      },
      testM("Не ходить в bigB если истории достаточно") {
        checkNM(1)(OwnerIdGen.anyOwnerId, Gen.listOfN(20)(SearchHistoryEntryGen.anySearchHistoryEntry)) {
          (owner, searches) =>
            for {
              _ <- TestBigBrotherClient.setMapping(Map.empty)
              _ <- Ydb.runTx(SearchHistoryDao.updateHistory(owner, searches))
              _ <- SearchHistoryManager.getSearchHistory(owner, 10)
            } yield assertCompletes
        }
      },
      testM("Отдавать категории холодного старта после очистки истории") {
        checkNM(1)(OwnerIdGen.anyOwnerId, Gen.listOfN(20)(SearchHistoryEntryGen.anySearchHistoryEntry)) {
          (owner, searches) =>
            for {
              _ <- TestBigBrotherClient.setMapping(Map.empty.withDefaultValue(profile))
              _ <- Ydb.runTx(SearchHistoryDao.updateHistory(owner, searches))
              _ <- SearchHistoryManager.clearSearchHistory(owner, ClearAll)
              (searches, suggests) <- SearchHistoryManager.getSearchHistory(owner, 10)
            } yield assert(searches)(isEmpty) &&
              assert(suggests.map(_.text))(
                hasSameElements(List(rootCategory1.name.toLowerCase, rootCategory2.name.toLowerCase))
              )
        }
      }
    )
  } @@ sequential).provideCustomLayerShared {
    val clock = Clock.live
    val ydb = TestYdb.ydb
    val txRunner = ydb >>> Ydb.txRunner
    val dao = ydb ++ clock >>> YdbSearchHistoryDao.live
    val bonsaiRef: ULayer[Has[Ref[PersonalBonsaiSnapshot]]] = Ref.make(TestPersonalBonsaiSnapshot.testSnapshot).toLayer
    val profileExtractor = bonsaiRef >>> BigbProfileExtractor.live

    val bigBClientMock = TestBigBrotherClient.layer

    clock ++ dao ++ txRunner ++ profileExtractor ++ bigBClientMock >+> SearchHistoryManager.live
  }
}
