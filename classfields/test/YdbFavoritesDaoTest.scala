package ru.yandex.vertis.general.favorites.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.common.model.user.OwnerId.UserId
import ru.yandex.vertis.general.favorites.model.SavedOffer
import ru.yandex.vertis.general.favorites.storage.FavoritesDao
import ru.yandex.vertis.general.favorites.storage.ydb.favorites.YdbFavoritesDao
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object YdbFavoritesDaoTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbFavoritesDaoTest")(
      testM("create SavedOffer") {
        val ownerId = UserId(id = 123)
        val saveOffer = List(SavedOffer(offerId = "offfer-id"))
        for {
          _ <- runTx(FavoritesDao.createOrUpdateFavorites(ownerId = ownerId, saveOffer))
          storedSavedOffer <- runTx(
            FavoritesDao.getFavorites[SavedOffer, String](ownerId, saveOffer.map(_.offerId).toSet)
          )
        } yield assert(storedSavedOffer("offfer-id"))(equalTo(saveOffer.head))

      }
    ).provideCustomLayer {
      TestYdb.ydb >>> (YdbFavoritesDao.live ++ Ydb.txRunner) ++ Clock.live
    } @@ sequential @@ shrinks(0)
  }
}
