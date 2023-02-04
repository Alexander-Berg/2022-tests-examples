package ru.yandex.vertis.general.bonsai.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.bonsai.category_model.Category
import general.bonsai.internal.internal_api.{PagingRequest, SearchParameters}
import ru.yandex.vertis.general.bonsai.model.EntityRef
import ru.yandex.vertis.general.bonsai.model.testkit.Generators
import ru.yandex.vertis.general.bonsai.storage.SuggestEntityIndexDao
import ru.yandex.vertis.general.bonsai.storage.SuggestEntityIndexDao.IndexedValue
import ru.yandex.vertis.general.bonsai.storage.ydb.YdbSuggestEntityIndexDao
import ru.yandex.vertis.general.bonsai.storage.testkit._
import zio.clock.Clock
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}

object YdbSuggestEntityIndexDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("YdbSuggestEntityIndexDao")(
      testM("Ищет все категории по тексту") {
        checkNM(1)(Generators.idGen, Generators.idGen) { (entityId1, entityId2) =>
          for {
            _ <- runTx(
              SuggestEntityIndexDao.update(
                added = List(
                  IndexedValue("массажная подушка для шеи", true, true, false),
                  IndexedValue("подушка для шеи", true, true, false),
                  IndexedValue("для шеи", true, true, false),
                  IndexedValue("шеи", true, true, false)
                ),
                removed = List.empty,
                EntityRef.forCategory(entityId1)
              )
            )
            _ <- runTx(
              SuggestEntityIndexDao.update(
                added = List(
                  IndexedValue("массажная подушка для шеи", true, true, true),
                  IndexedValue("подушка для шеи", true, false, true),
                  IndexedValue("для шеи", false, true, false),
                  IndexedValue("шеи", true, true, false)
                ),
                removed = List.empty,
                EntityRef.forCategory(entityId2)
              )
            )
            all <- runTx(SuggestEntityIndexDao.search[Category](SearchParameters(searchString = "мас")))
            onlyNames <- runTx(
              SuggestEntityIndexDao.search[Category](SearchParameters(searchString = "для", inNameOnly = true))
            )
            onlyActive <- runTx(
              SuggestEntityIndexDao.search[Category](SearchParameters(searchString = "под", onlyActive = true))
            )
            skipSymlinks <- runTx(
              SuggestEntityIndexDao.search[Category](SearchParameters(searchString = "мас", skipSymlink = true))
            )
          } yield assert(all)(
            hasSameElements(
              Seq(
                (IndexedValue("массажная подушка для шеи", true, true, false), EntityRef.forCategory(entityId1)),
                (IndexedValue("массажная подушка для шеи", true, true, true), EntityRef.forCategory(entityId2))
              )
            )
          ) &&
            assert(onlyNames)(
              hasSameElements(
                Seq((IndexedValue("для шеи", true, true, false), EntityRef.forCategory(entityId1)))
              )
            ) &&
            assert(onlyActive)(
              hasSameElements(
                Seq((IndexedValue("подушка для шеи", true, true, false), EntityRef.forCategory(entityId1)))
              )
            ) &&
            assert(skipSymlinks)(
              hasSameElements(
                Seq((IndexedValue("массажная подушка для шеи", true, true, false), EntityRef.forCategory(entityId1)))
              )
            )
        }
      }
    ) @@ sequential @@ shrinks(0)).provideCustomLayerShared {
      val ydb = TestYdb.ydb
      val txRunner = ydb >>> Ydb.txRunner
      ydb >>> (YdbSuggestEntityIndexDao.live ++ txRunner ++ Clock.live)
    }
  }
}
