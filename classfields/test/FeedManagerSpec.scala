package ru.yandex.vertis.general.feed.logic.test

import common.zio.doobie.testkit.TestPostgresql
import common.zio.testkit.Checks
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.logic.FeedManager
import ru.yandex.vertis.general.feed.logic.test.FeedTestInitUtils._
import ru.yandex.vertis.general.feed.model.testkit.{FeedSettingsGen, NamespaceIdGen}
import ru.yandex.vertis.general.feed.model.{Feed, FeedSettings, FeedStatus}
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._

object FeedManagerSpec extends DefaultRunnableSpec {

  private def withVersion(version: Long) =
    hasField[Feed, Long]("version", _.version, equalTo(version))

  private def withSettings(settings: FeedSettings) =
    hasField[Feed, FeedSettings]("settings", _.settings, equalTo(settings: FeedSettings))

  private def withStatus(status: FeedStatus) =
    hasField[Feed, FeedStatus]("status", _.status, equalTo(status))

  private def assertFeed(
      feed: Option[Feed]
    )(version: Long,
      settings: FeedSettings,
      status: FeedStatus): TestResult = {
    assert(feed)(
      isSome(
        withSettings(settings) && withVersion(version) && withStatus(status)
      )
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("FeedManager")(
      testM("update & get feeds") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          FeedSettingsGen.anyFeedSettings.noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink,
          FeedSettingsGen.anyFeedSettings.noShrink
        ) { (sellerId, namespaceId, feedSettings, namespaceId2, feedSettings2) =>
          for {
            _ <- FeedManager.updateFeed(sellerId, namespaceId, feedSettings)
            _ <- FeedManager.updateFeed(sellerId, namespaceId2, feedSettings2)
            current <- FeedManager.getFeed(sellerId, namespaceId)
            current2 <- FeedManager.getFeed(sellerId, namespaceId2)
          } yield assert(current)(
            isSome(withSettings(feedSettings) && withVersion(0L) && withStatus(FeedStatus.Active))
          ) &&
            assert(current2)(isSome(withSettings(feedSettings2) && withVersion(0L) && withStatus(FeedStatus.Active)))
        }
      },
      testM("increment version on updates") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink,
          FeedSettingsGen.anyFeedSettings.noShrink,
          FeedSettingsGen.anyFeedSettings.noShrink,
          FeedSettingsGen.anyFeedSettings.noShrink
        ) { (sellerId, namespaceId, namespaceId2, firstFeedSettings, secondFeedSettings, thirdFeedSettings) =>
          for {
            _ <- FeedManager.updateFeed(sellerId, namespaceId, firstFeedSettings)
            _ <- FeedManager.updateFeed(sellerId, namespaceId, secondFeedSettings)
            _ <- FeedManager.updateFeed(sellerId, namespaceId2, thirdFeedSettings)
            _ <- FeedManager.updateFeed(sellerId, namespaceId2, thirdFeedSettings)
            current <- FeedManager.getFeed(sellerId, namespaceId)
            current2 <- FeedManager.getFeed(sellerId, namespaceId2)
          } yield assert(current)(
            isSome(withSettings(secondFeedSettings) && withVersion(1L) && withStatus(FeedStatus.Active))
          ) && assert(current2)(
            isSome(withSettings(thirdFeedSettings) && withVersion(1L) && withStatus(FeedStatus.Active))
          )
        }
      },
      testM("increment version on deletes") {
        Checks
          .CheckMn(1)
          .apply(
            SellerGen.anySellerId.noShrink,
            NamespaceIdGen.anyNamespaceId("1").noShrink,
            NamespaceIdGen.anyNamespaceId("2").noShrink,
            FeedSettingsGen.anyFeedSettings.noShrink,
            FeedSettingsGen.anyFeedSettings.noShrink,
            FeedSettingsGen.anyFeedSettings.noShrink,
            FeedSettingsGen.anyFeedSettings.noShrink
          ) {
            (
                sellerId,
                namespaceId1,
                namespaceId2,
                firstFeedSettings1,
                secondFeedSettings1,
                firstFeedSettings2,
                secondFeedSettings2
            ) =>
              for {
                _ <- FeedManager.updateFeed(sellerId, namespaceId1, firstFeedSettings1)
                _ <- FeedManager.updateFeed(sellerId, namespaceId2, firstFeedSettings2)

                _ <- FeedManager.deleteFeed(sellerId, namespaceId1)
                _ <- FeedManager.deleteFeed(sellerId, namespaceId2)

                deleted1 <- FeedManager.getFeed(sellerId, namespaceId1, includeRemoved = true)
                deleted2 <- FeedManager.getFeed(sellerId, namespaceId2, includeRemoved = true)

                _ <- FeedManager.updateFeed(sellerId, namespaceId1, secondFeedSettings1)
                _ <- FeedManager.updateFeed(sellerId, namespaceId2, secondFeedSettings2)

                current1 <- FeedManager.getFeed(sellerId, namespaceId1)
                current2 <- FeedManager.getFeed(sellerId, namespaceId2)

              } yield {
                assertFeed(deleted1)(1L, firstFeedSettings1, FeedStatus.Removed) &&
                assertFeed(deleted2)(1L, firstFeedSettings2, FeedStatus.Removed) &&
                assertFeed(current1)(2L, secondFeedSettings1, FeedStatus.Active) &&
                assertFeed(current2)(2L, secondFeedSettings2, FeedStatus.Active)
              }
          }
      },
      testM("delete existing feeds") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId().noShrink,
          FeedSettingsGen.anyFeedSettings.noShrink
        ) { (sellerId, namespaceId, feedSettings) =>
          for {
            _ <- FeedManager.updateFeed(sellerId, namespaceId, feedSettings)
            _ <- FeedManager.deleteFeed(sellerId, namespaceId)
            currentWithoutRemoved <- FeedManager.getFeed(sellerId, namespaceId)
            currentWithRemoved <- FeedManager.getFeed(sellerId, namespaceId, includeRemoved = true)
          } yield assert(currentWithRemoved)(isSome(withVersion(1L) && withStatus(FeedStatus.Removed))) &&
            assert(currentWithoutRemoved)(isNone)
        }
      },
      testM("not fail delete if feed doesn't exist") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink) {
          (sellerId, namespaceId) =>
            for {
              _ <- FeedManager.deleteFeed(sellerId, namespaceId)
              current <- FeedManager.getFeed(sellerId, namespaceId)
            } yield assert(current)(isNone)
        }
      }
    ) @@ sequential)
      .provideCustomLayerShared {
        Random.live ++ feedManager
      }
  }
}
