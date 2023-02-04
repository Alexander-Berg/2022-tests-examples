package ru.yandex.vertis.general.personal.logic.test

import ru.yandex.proto.crypta.user_profile.Profile
import ru.yandex.proto.crypta.user_profile.Profile.Counter
import ru.yandex.vertis.general.bonsai.utils.CategoryHashUtil
import ru.yandex.vertis.general.common.model.user.OwnerId
import ru.yandex.vertis.general.personal.logic.{
  BigbProfileExtractor,
  DefaultRecommendedCategoriesManager,
  PersonalBonsaiSnapshot,
  PersonalOfferCountSnapshot,
  RecommendedCategoriesManager
}
import ru.yandex.vertis.general.personal.testkit.TestPersonalBonsaiSnapshot
import common.zio.logging.Logging
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{Has, Ref, Task, ULayer, ZLayer}
import zio.test._
import zio.test.Assertion._
import ru.yandex.vertis.general.personal.logic.DefaultBigbProfileExtractor._
import CategoryHashUtil._
import TestPersonalBonsaiSnapshot._
import cats.data.NonEmptyList
import common.clients.bigb.model.BigBrotherUserId
import common.clients.bigb.testkit.TestBigBrotherClient

object RecommendedCategoriesManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("RecommendedCategoriesManagerTest")(
      testM("сортировать корневые категории с учетом данных биг б") {
        val profile = Profile(counters =
          Seq(
            Counter(
              counterId = Some(ViewedCategoriesTimedCounterId.counterId),
              key = Seq(
                getCategoryIdHash(leafCategory1.id),
                getCategoryIdHash(leafCategory2.id),
                getCategoryIdHash(leafCategory3.id)
              ),
              value = Seq(0, 0, 0)
            ),
            Counter(
              counterId = Some(ViewedCategoriesTimedCounterId.counterTimeId),
              key = Seq(
                getCategoryIdHash(leafCategory1.id),
                getCategoryIdHash(leafCategory2.id),
                getCategoryIdHash(leafCategory3.id)
              ),
              value = Seq(1.0, 2.0, 3.0)
            ),
            Counter(
              counterId = Some(ColdStartCategoriesTimedCounterId.counterId),
              key = Seq(
                getCategoryIdHash(rootCategory4.id)
              ),
              value = Seq(0)
            ),
            Counter(
              counterId = Some(ColdStartCategoriesTimedCounterId.counterTimeId),
              key = Seq(
                getCategoryIdHash(rootCategory4.id)
              ),
              value = Seq(1.0)
            )
          )
        )
        for {
          _ <- TestBigBrotherClient.addMapping(Map(NonEmptyList.one(BigBrotherUserId.PassportUid(1)) -> profile))
          categories <- RecommendedCategoriesManager.getRecommendedRootCategories(
            ownerId = OwnerId.UserId(id = 1, anonymous = None)
          )
        } yield assert(categories)(
          equalTo(
            List(
              rootCategory2,
              rootCategory1,
              rootCategory4,
              rootCategory3,
              rootCategory5
            )
          )
        )
      },
      testM("рекомендуемые категории из биг б (последние просмотренные, холодный старт, фолбэчные") {
        val profile = Profile(counters =
          Seq(
            Counter(
              counterId = Some(ViewedCategoriesTimedCounterId.counterId),
              key = Seq(
                getCategoryIdHash(leafCategory1.id),
                getCategoryIdHash(leafCategory2.id),
                getCategoryIdHash(leafCategory3.id),
                getCategoryIdHash(leafCategory4.id)
              ),
              value = Seq(0, 0, 0, 0)
            ),
            Counter(
              counterId = Some(ViewedCategoriesTimedCounterId.counterTimeId),
              key = Seq(
                getCategoryIdHash(leafCategory1.id),
                getCategoryIdHash(leafCategory2.id),
                getCategoryIdHash(leafCategory3.id),
                getCategoryIdHash(leafCategory4.id)
              ),
              value = Seq(1.0, 2.0, 3.0, 4.0)
            ),
            Counter(
              counterId = Some(ColdStartCategoriesTimedCounterId.counterId),
              key = Seq(
                getCategoryIdHash(rootCategory4.id)
              ),
              value = Seq(0)
            ),
            Counter(
              counterId = Some(ColdStartCategoriesTimedCounterId.counterTimeId),
              key = Seq(
                getCategoryIdHash(rootCategory4.id)
              ),
              value = Seq(1.0)
            )
          )
        )
        for {
          _ <- TestBigBrotherClient.addMapping(Map(NonEmptyList.one(BigBrotherUserId.PassportUid(2)) -> profile))
          categories <- RecommendedCategoriesManager.getRecommendedCategories(
            ownerId = OwnerId.UserId(id = 2, anonymous = None),
            regionId = None,
            categoriesLimit = Some(15),
            offersLimit = None
          )
        } yield assert(categories)(
          equalTo(
            List(
              leafCategory3.id,
              leafCategory2.id,
              leafCategory1.id,
              rootCategory4.id
            ) ++ DefaultRecommendedCategoriesManager.FallbackCategories
          )
        )
      },
      testM("рекомендуемые категории с учетом количества объявлений в категориях") {
        val profile = Profile(counters =
          Seq(
            Counter(
              counterId = Some(ViewedCategoriesTimedCounterId.counterId),
              key = Seq(
                getCategoryIdHash(leafCategory1.id),
                getCategoryIdHash(leafCategory2.id),
                getCategoryIdHash(leafCategory3.id),
                getCategoryIdHash(leafCategory4.id)
              ),
              value = Seq(0, 0, 0, 0)
            ),
            Counter(
              counterId = Some(ViewedCategoriesTimedCounterId.counterTimeId),
              key = Seq(
                getCategoryIdHash(leafCategory1.id),
                getCategoryIdHash(leafCategory2.id),
                getCategoryIdHash(leafCategory3.id),
                getCategoryIdHash(leafCategory4.id)
              ),
              value = Seq(1.0, 2.0, 3.0, 4.0)
            ),
            Counter(
              counterId = Some(ColdStartCategoriesTimedCounterId.counterId),
              key = Seq(
                getCategoryIdHash(rootCategory4.id)
              ),
              value = Seq(0)
            ),
            Counter(
              counterId = Some(ColdStartCategoriesTimedCounterId.counterTimeId),
              key = Seq(
                getCategoryIdHash(rootCategory4.id)
              ),
              value = Seq(1.0)
            )
          )
        )
        for {
          _ <- TestBigBrotherClient.addMapping(Map(NonEmptyList.one(BigBrotherUserId.PassportUid(2)) -> profile))
          categories <- RecommendedCategoriesManager.getRecommendedCategories(
            ownerId = OwnerId.UserId(id = 2, anonymous = None),
            regionId = None,
            categoriesLimit = Some(1),
            offersLimit = Some(100)
          )
        } yield assert(categories)(
          equalTo(
            List(
              leafCategory3.id,
              leafCategory2.id,
              leafCategory1.id,
              rootCategory4.id
            ) ++ DefaultRecommendedCategoriesManager.FallbackCategories.take(6)
          )
        )
      },
      testM("возвращает последние просмотренные объявления") {
        val profile = Profile(counters =
          Seq(
            Counter(
              counterId = Some(ViewedOffersTimedCounterId.counterId),
              key = Seq(1, 2, 3),
              value = Seq(0, 0, 0)
            ),
            Counter(
              counterId = Some(ViewedOffersTimedCounterId.counterTimeId),
              key = Seq(1, 2, 3),
              value = Seq(1.0, 2.0, 3.0)
            )
          )
        )
        for {
          _ <- TestBigBrotherClient.addMapping(Map(NonEmptyList.one(BigBrotherUserId.PassportUid(3)) -> profile))
          offersId <- RecommendedCategoriesManager.getLastViewedOffers(
            ownerId = OwnerId.UserId(id = 3, anonymous = None),
            limit = None
          )
        } yield assert(offersId)(
          equalTo(List("3", "2", "1"))
        )
      }
    )
  }.provideCustomLayerShared {
    val logger = Logging.live
    val clock = Clock.live
    val blocking = Blocking.live
    val base = logger ++ clock ++ blocking
    val bigBClientMock = TestBigBrotherClient.layer
    val offerCountSnapshotMock = PersonalOfferCountSnapshot.withCount(10)

    val bonsaiRef: ULayer[Has[Ref[PersonalBonsaiSnapshot]]] = Ref.make(TestPersonalBonsaiSnapshot.testSnapshot).toLayer

    val profileExtractor = bonsaiRef >>> BigbProfileExtractor.live
    val manager =
      base ++ profileExtractor ++ bigBClientMock ++ offerCountSnapshotMock ++ bonsaiRef >>> RecommendedCategoriesManager.live
    manager ++ bigBClientMock
  }
}
