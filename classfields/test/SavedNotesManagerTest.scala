package ru.yandex.vertis.general.favorites.logic.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.favorites.logic.{FavoritesStore, SavedNotesManager}
import ru.yandex.vertis.general.favorites.model.testkit.SavedNoteGen
import ru.yandex.vertis.general.favorites.storage.ydb.counts.YdbFavoritesCountDao
import ru.yandex.vertis.general.favorites.storage.ydb.favorites.YdbFavoritesDao
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.TestAspect.{before, sequential, shrinks}
import zio.test.{assert, checkNM, DefaultRunnableSpec, ZSpec}

object SavedNotesManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SavedNotesManagerTest")(
      testM("save & get & delete SavedNotes") {
        checkNM(1)(SavedNoteGen.anySavedNotes(15), OwnerIdGen.anyUserId) { (savedNotes, ownerId) =>
          val foreignIds = Set("foreign1", "foreign2")
          for {
            _ <- SavedNotesManager.createOrUpdateSavedNotes(userId = ownerId, savedNotes)
            stored <- SavedNotesManager.getSavedNotes(ownerId, savedNotes.map(_.offerId).toSet)
            _ <- SavedNotesManager.deleteSavedNotes(
              ownerId,
              (savedNotes.map(_.offerId) ++ foreignIds).toSet
            )
            afterDelete <- SavedNotesManager.getSavedNotes(ownerId, savedNotes.map(_.offerId).toSet)
          } yield assert(stored.toSet)(equalTo(savedNotes.toSet)) &&
            assert(afterDelete)(equalTo(List.empty))
        }

      }
    ) @@ before(runTx(YdbFavoritesDao.clean) *> runTx(YdbFavoritesCountDao.clean)) @@ sequential @@ shrinks(0)
  }.provideCustomLayerShared {
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val clock = Clock.live
    val favoritesDao = TestYdb.ydb >+> YdbFavoritesDao.live
    val favoritesCountDao = TestYdb.ydb >+> YdbFavoritesCountDao.live
    val favoritesStore = (favoritesDao ++ favoritesCountDao) >+> FavoritesStore.live
    val savedNotesManager = (favoritesStore ++ txRunner ++ clock) >+> SavedNotesManager.live
    favoritesStore ++ txRunner ++ clock ++ savedNotesManager
  }

}
