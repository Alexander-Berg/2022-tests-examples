package ru.yandex.vertis.lenta.feed

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.concurrent.ScalaFutures
import ru.auto.lenta.ContentOuterClass.{Content, ContentSource, Payload}
import ru.yandex.vertis.lenta.api.components.cache.feed.UserIdSource
import ru.yandex.vertis.lenta.api.components.cache.{CacheWrapper, EhcacheManager}
import ru.yandex.vertis.lenta.api.model.Feed
import ru.yandex.vertis.lenta.model.UserReads
import ru.yandex.vertis.lenta.{ContentID, UserID}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.Model.Query
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

class FeedBuilderTest extends AnyFunSuite with ScalaFutures with MockitoSupport {

  val mockEhcacheManager: EhcacheManager = mock[EhcacheManager]
  val mockFeedCache: CacheWrapper[UserIdSource, Feed] = mock[CacheWrapper[UserIdSource, Feed]]
  val mockContentCache: CacheWrapper[ContentID, Content] = mock[CacheWrapper[ContentID, Content]]
  val mockUserDataCache: CacheWrapper[UserID, Query] = mock[CacheWrapper[UserID, Query]]
  implicit val t: Traced = Traced.empty

  val feedBuilder = new FeedBuilder(mockEhcacheManager)
  val userId = "user:123"
  val initialContentId = 0
  val maxContentId = 9
  val contentAmount = 10
  val source = ContentSource.ALL
  val userIdSource = UserIdSource(userId, source)

  when(mockEhcacheManager.getFeedCache).thenReturn(mockFeedCache)
  when(mockEhcacheManager.getContentCache).thenReturn(mockContentCache)
  when(mockEhcacheManager.getUserDataCache).thenReturn(mockUserDataCache)

  test("buildFeed without contentId, all unread, contentAmount more than actual content count") {
    val contentAmount = 10
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)

    val unreadUserReads = makeUserReads(0, 3)

    val expectedIdsOrder = List(
      "magazine_0",
      "magazine_1",
      "magazine_2",
      "magazine_3"
    )

    val feed = Feed(unread = unreadUserReads, previewed = List.empty, read = List.empty)

    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val response = feedBuilder.buildFeed(userId, None, contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)
    assert(response.getPayloadsList.asScala.map(_.getId).equals(expectedIdsOrder))

    assert(response.getPayloadsList.asScala.count(_.getWasSeenPreview) == 0)
    assert(response.getPayloadsList.asScala.count(_.getWasRead) == 0)

    assert(response.getPageStatistics.getTotalCount == 4)
    assert(response.getPageStatistics.getShownContentCount == expectedIdsOrder.size)
    assert(response.getPageStatistics.getFromContentId == expectedIdsOrder.head)
  }

  test("buildFeed without contentId, all unread") {
    val contentAmount = 3
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)

    val unreadUserReads = makeUserReads(0, 3)

    val expectedIdsOrder = List(
      "magazine_0",
      "magazine_1",
      "magazine_2"
    )

    val feed = Feed(unread = unreadUserReads, previewed = List.empty, read = List.empty)

    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val response = feedBuilder.buildFeed(userId, None, contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)
    assert(response.getPayloadsList.asScala.map(_.getId).equals(expectedIdsOrder))

    assert(response.getPayloadsList.asScala.count(_.getWasSeenPreview) == 0)
    assert(response.getPayloadsList.asScala.count(_.getWasRead) == 0)

    assert(response.getPageStatistics.getTotalCount == 4)
    assert(response.getPageStatistics.getShownContentCount == expectedIdsOrder.size)
    assert(response.getPageStatistics.getFromContentId == expectedIdsOrder.head)
  }

  test("buildFeed without contentId, unread + previewed + read") {
    val contentAmount = 10
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)

    val unreadUserReads = makeUserReads(7, 9)
    val previewedUserReads = makeUserReads(10, 12, wasPreviewed = true)
    val readUserReads = makeUserReads(1, 5, wasPreviewed = true, wasRead = true)

    val feed = Feed(unread = unreadUserReads, previewed = previewedUserReads, read = readUserReads)
    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val expectedIdsOrder = List(
      "magazine_7",
      "magazine_8",
      "magazine_9",
      "magazine_10",
      "magazine_11",
      "magazine_12",
      "magazine_1",
      "magazine_2",
      "magazine_3",
      "magazine_4"
    )
    val response = feedBuilder.buildFeed(userId, None, contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)
    assert(response.getPayloadsList.asScala.map(_.getId).equals(expectedIdsOrder))

    assert(response.getPayloadsList.asScala.count(_.getWasSeenPreview) == 7)
    assert(response.getPayloadsList.asScala.count(_.getWasRead) == 4)

    assert(response.getPageStatistics.getTotalCount == 11)
    assert(response.getPageStatistics.getShownContentCount == expectedIdsOrder.size)
    assert(response.getPageStatistics.getFromContentId == expectedIdsOrder.head)
  }

  test("buildFeed with contentId, unread + previewed + read") {
    val contentAmount = 10
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)
    val contentId = "magazine_10"

    val unreadUserReads = makeUserReads(7, 9)
    val previewedUserReads = makeUserReads(10, 12, wasPreviewed = true)
    val readUserReads = makeUserReads(1, 5, wasPreviewed = true, wasRead = true)

    val feed = Feed(unread = unreadUserReads, previewed = previewedUserReads, read = readUserReads)
    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val expectedIdsOrder = List(
      "magazine_11",
      "magazine_12",
      "magazine_1",
      "magazine_2",
      "magazine_3",
      "magazine_4",
      "magazine_5"
    )
    val response = feedBuilder.buildFeed(userId, Some(contentId), contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)
    assert(response.getPayloadsList.asScala.map(_.getId).equals(expectedIdsOrder))

    assert(response.getPayloadsList.asScala.count(_.getWasSeenPreview) == 7)
    assert(response.getPayloadsList.asScala.count(_.getWasRead) == 5)

    assert(response.getPageStatistics.getTotalCount == 11)
    assert(response.getPageStatistics.getShownContentCount == expectedIdsOrder.size)
    assert(response.getPageStatistics.getFromContentId == contentId)
  }

  test("buildFeed without contentId, unread + previewed + read, a lot of pessimized") {
    val contentAmount = 2
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)

    val unreadUserReads = makeUserReads(5, 5) ++ makeUserReads(9, 10, forPessimization = true)
    val previewedUserReads = makeUserReads(6, 8, wasPreviewed = true)
    val readUserReads = makeUserReads(1, 4, wasPreviewed = true, wasRead = true)

    val feed = Feed(unread = unreadUserReads, previewed = previewedUserReads, read = readUserReads)
    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val expectedIdsOrder = List(
      "magazine_5",
      "magazine_6"
    )
    val response = feedBuilder.buildFeed(userId, None, contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)
    assert(response.getPayloadsList.asScala.map(_.getId).equals(expectedIdsOrder))

    assert(response.getPayloadsList.asScala.count(_.getWasSeenPreview) == 1)
    assert(response.getPayloadsList.asScala.count(_.getWasRead) == 0)

    assert(response.getPageStatistics.getTotalCount == 8)
    assert(response.getPageStatistics.getShownContentCount == expectedIdsOrder.size)
    assert(response.getPageStatistics.getFromContentId == expectedIdsOrder.head)
  }

  test("buildFeed with contentId, unread only, with a lot of pessimized") {
    val contentAmount = 3
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)
    val contentId = "magazine_3"

    val unreadUserReads =
      makeUserReads(1, 4, forPessimization = true) ++
        makeUserReads(5, 5) ++
        makeUserReads(9, 10, forPessimization = true)

    val feed = Feed(unread = unreadUserReads, previewed = List.empty, read = List.empty)
    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val expectedIdsOrder = List(
      "magazine_5"
    )
    val response = feedBuilder.buildFeed(userId, Some(contentId), contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)
    assert(response.getPayloadsList.asScala.map(_.getId).equals(expectedIdsOrder))

    assert(response.getPayloadsList.asScala.count(_.getWasSeenPreview) == 0)
    assert(response.getPayloadsList.asScala.count(_.getWasRead) == 0)

    assert(response.getPageStatistics.getTotalCount == 1)
    assert(response.getPageStatistics.getShownContentCount == expectedIdsOrder.size)
    assert(response.getPageStatistics.getFromContentId == contentId)
  }

  test("buildFeed with contentId, unread only, with pessimized") {
    val contentAmount = 3
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)
    val contentId = "magazine_3"

    val unreadUserReads =
      makeUserReads(1, 4, forPessimization = true) ++
        makeUserReads(5, 8) ++
        makeUserReads(9, 10, forPessimization = true)

    val feed = Feed(unread = unreadUserReads, previewed = List.empty, read = List.empty)
    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val expectedIdsOrder = List(
      "magazine_5",
      "magazine_6",
      "magazine_7"
    )
    val response = feedBuilder.buildFeed(userId, Some(contentId), contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)
    assert(response.getPayloadsList.asScala.map(_.getId).equals(expectedIdsOrder))

    assert(response.getPayloadsList.asScala.count(_.getWasSeenPreview) == 0)
    assert(response.getPayloadsList.asScala.count(_.getWasRead) == 0)

    assert(response.getPageStatistics.getTotalCount == 4)
    assert(response.getPageStatistics.getShownContentCount == expectedIdsOrder.size)
    assert(response.getPageStatistics.getFromContentId == contentId)
  }

  test("buildFeed with contentId, unread only, all pessimized") {
    val contentAmount = 3
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)
    val contentId = "magazine_3"

    val unreadUserReads =
      makeUserReads(1, 4, forPessimization = true)

    val feed = Feed(unread = unreadUserReads, previewed = List.empty, read = List.empty)
    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val response = feedBuilder.buildFeed(userId, Some(contentId), contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == 0)

    assert(response.getPageStatistics.getTotalCount == 0)
    assert(response.getPageStatistics.getShownContentCount == 0)
    assert(response.getPageStatistics.getFromContentId == contentId)
  }

  test("buildFeed without contentId, unread only, all pessimized") {
    val contentAmount = 3
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)

    val unreadUserReads =
      makeUserReads(1, 4, forPessimization = true)

    val feed = Feed(unread = unreadUserReads, previewed = List.empty, read = List.empty)
    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val response = feedBuilder.buildFeed(userId, None, contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == 0)

    assert(response.getPageStatistics.getTotalCount == 0)
    assert(response.getPageStatistics.getShownContentCount == 0)
    assert(response.getPageStatistics.getFromContentId.isEmpty)
  }

  test("buildFeed with contentId, no content") {
    val contentAmount = 3
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)
    val contentId = "magazine_3"

    val feed = Feed(unread = List.empty, previewed = List.empty, read = List.empty)
    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val response = feedBuilder.buildFeed(userId, Some(contentId), contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == 0)

    assert(response.getPageStatistics.getTotalCount == 0)
    assert(response.getPageStatistics.getShownContentCount == 0)
    assert(response.getPageStatistics.getFromContentId == contentId)
  }

  test("buildFeed without contentId, no content") {
    val contentAmount = 3
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)

    val feed = Feed(unread = List.empty, previewed = List.empty, read = List.empty)
    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val response = feedBuilder.buildFeed(userId, None, contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == 0)

    assert(response.getPageStatistics.getTotalCount == 0)
    assert(response.getPageStatistics.getShownContentCount == 0)
    assert(response.getPageStatistics.getFromContentId.isEmpty)
  }

  test("buildFeed with non existing contentId, all unread") {
    val contentAmount = 10
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)
    val contentId = "magazine_nonexist"

    val unreadUserReads = makeUserReads(0, 3)

    val expectedIdsOrder = List(
      "magazine_0",
      "magazine_1",
      "magazine_2",
      "magazine_3"
    )

    val feed = Feed(unread = unreadUserReads, previewed = List.empty, read = List.empty)

    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val response = feedBuilder.buildFeed(userId, Some(contentId), contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)
    assert(response.getPayloadsList.asScala.map(_.getId).equals(expectedIdsOrder))

    assert(response.getPayloadsList.asScala.count(_.getWasSeenPreview) == 0)
    assert(response.getPayloadsList.asScala.count(_.getWasRead) == 0)

    assert(response.getPageStatistics.getTotalCount == 4)
    assert(response.getPageStatistics.getShownContentCount == expectedIdsOrder.size)
    assert(response.getPageStatistics.getFromContentId == contentId)
  }

  test("buildFeedForCache, pessimizeViewedContent=false") {
    val oldUnreadUserReads = makeUserReads(6, 8)
    val oldPreviewedUserReads = makeUserReads(3, 5, wasPreviewed = true)
    val oldReadUserReads = makeUserReads(1, 2, wasPreviewed = true, wasRead = true)
    val oldFeed =
      Feed(unread = oldUnreadUserReads ++ oldPreviewedUserReads, previewed = List.empty, read = oldReadUserReads)

    val newPreviewedUserReads = makeUserReads(6, 7, wasPreviewed = true)
    val newReadUserReads = makeUserReads(4, 6, wasPreviewed = true, wasRead = true)
    val newUserReads = newReadUserReads ++ newPreviewedUserReads

    val feed = FeedBuilder.buildFeedForCache(newUserReads, oldFeed, pessimizeViewedContent = false)

    val expectedUnreadIdsOrder = List(
      "magazine_6",
      "magazine_7",
      "magazine_8",
      "magazine_3",
      "magazine_4",
      "magazine_5"
    )
    val expectedReadIdsOrder = List(
      "magazine_1",
      "magazine_2",
      "magazine_4",
      "magazine_5",
      "magazine_6"
    )

    assert(feed.unread.size == expectedUnreadIdsOrder.size)
    assert(feed.unread.map(_.contentId).equals(expectedUnreadIdsOrder))

    assert(feed.previewed.isEmpty)

    assert(feed.read.size == expectedReadIdsOrder.size)
    assert(feed.read.map(_.contentId).equals(expectedReadIdsOrder))

    assert(
      feed.unread.filter(_.forPessimization).map(_.contentId).toSet.equals(newReadUserReads.map(_.contentId).toSet)
    )
    val allPreviewedUserReads = newPreviewedUserReads ++ oldPreviewedUserReads
    assert(
      feed.unread.filter(_.wasSeenPreview).map(_.contentId).toSet.equals(allPreviewedUserReads.map(_.contentId).toSet)
    )
  }

  test("buildFeedForCache, pessimizeViewedContent=true") {
    val oldUnreadUserReads = makeUserReads(6, 8)
    val oldPreviewedUserReads = makeUserReads(3, 5, wasPreviewed = true)
    val oldReadUserReads = makeUserReads(1, 2, wasPreviewed = true, wasRead = true)
    val oldFeed =
      Feed(unread = oldUnreadUserReads, previewed = oldPreviewedUserReads, read = oldReadUserReads)

    val newPreviewedUserReads = makeUserReads(6, 7, wasPreviewed = true)
    val newReadUserReads = makeUserReads(4, 6, wasPreviewed = true, wasRead = true)
    val newUserReads = newReadUserReads ++ newPreviewedUserReads

    val feed = FeedBuilder.buildFeedForCache(newUserReads, oldFeed, pessimizeViewedContent = true)

    val expectedUnreadIdsOrder = List(
      "magazine_6",
      "magazine_7",
      "magazine_8"
    )
    val expectedPreviewedIdsOrder = List(
      "magazine_3",
      "magazine_4",
      "magazine_5",
      "magazine_6",
      "magazine_7"
    )
    val expectedReadIdsOrder = List(
      "magazine_1",
      "magazine_2",
      "magazine_4",
      "magazine_5",
      "magazine_6"
    )

    val expectedPessimizedInUnread = Set("magazine_6", "magazine_7")
    val expectedPessimizedInPreviewed = Set(
      "magazine_4",
      "magazine_5",
      "magazine_6"
    )

    assert(feed.unread.size == expectedUnreadIdsOrder.size)
    assert(feed.unread.map(_.contentId).equals(expectedUnreadIdsOrder))

    assert(feed.previewed.size == expectedPreviewedIdsOrder.size)
    assert(feed.previewed.map(_.contentId).equals(expectedPreviewedIdsOrder))

    assert(feed.read.size == expectedReadIdsOrder.size)
    assert(feed.read.map(_.contentId).equals(expectedReadIdsOrder))

    assert(feed.unread.filter(_.forPessimization).map(_.contentId).toSet.equals(expectedPessimizedInUnread))
    assert(feed.previewed.filter(_.forPessimization).map(_.contentId).toSet.equals(expectedPessimizedInPreviewed))
  }

  test("buildFeedFromYoctoResponse, pessimizeViewedContent=false") {
    val previewedUserReads = makeUserReads(1, 3, wasPreviewed = true)
    val readUserReads = makeUserReads(4, 5, wasPreviewed = true, wasRead = true)
    val allUserReads = previewedUserReads ++ readUserReads

    val contentIds = for (id <- (1 to 8).toList) yield s"magazine_$id"

    val feed = FeedBuilder.buildFeedFromYoctoResponse(userId, allUserReads, contentIds, pessimizeViewedContent = false)

    val expectedUnreadSize = contentIds.size - readUserReads.size
    val expectedUnreadIdsOrder = List(
      "magazine_1",
      "magazine_2",
      "magazine_3",
      "magazine_6",
      "magazine_7",
      "magazine_8"
    )

    val expectedReadIdsOrder = List(
      "magazine_4",
      "magazine_5"
    )

    assert(feed.unread.size == expectedUnreadSize)
    assert(feed.unread.map(_.contentId) == expectedUnreadIdsOrder)

    assert(feed.previewed.isEmpty)

    assert(feed.read.size == readUserReads.size)
    assert(feed.read.map(_.contentId) == expectedReadIdsOrder)
  }

  test("buildFeedFromYoctoResponse, pessimizeViewedContent=true") {
    val previewedUserReads = makeUserReads(1, 3, wasPreviewed = true)
    val readUserReads = makeUserReads(4, 5, wasPreviewed = true, wasRead = true)
    val allUserReads = previewedUserReads ++ readUserReads

    val contentIds = for (id <- (1 to 8).toList) yield s"magazine_$id"

    val feed = FeedBuilder.buildFeedFromYoctoResponse(userId, allUserReads, contentIds, pessimizeViewedContent = true)

    val expectedUnreadIdsOrder = List(
      "magazine_6",
      "magazine_7",
      "magazine_8"
    )

    val expectedPreviewedIdsOrder = List(
      "magazine_1",
      "magazine_2",
      "magazine_3"
    )

    val expectedReadIdsOrder = List(
      "magazine_4",
      "magazine_5"
    )

    assert(feed.unread.size == expectedUnreadIdsOrder.size)
    assert(feed.unread.map(_.contentId) == expectedUnreadIdsOrder)

    assert(feed.previewed.size == expectedPreviewedIdsOrder.size)
    assert(feed.previewed.map(_.contentId) == expectedPreviewedIdsOrder)

    assert(feed.read.size == readUserReads.size)
    assert(feed.read.map(_.contentId) == expectedReadIdsOrder)
  }

  private def makeUserReads(
      fromId: Int,
      toId: Int,
      wasPreviewed: Boolean = false,
      wasRead: Boolean = false,
      forPessimization: Boolean = false): List[UserReads] = {
    val ids = (fromId to toId).toList
    val userReads =
      for (id <- ids)
        yield UserReads(
          userId = userId,
          contentId = s"magazine_$id",
          wasSeenPreview = wasPreviewed,
          wasRead = wasRead,
          forPessimization = forPessimization
        )
    val content =
      for (id <- ids)
        yield Content
          .newBuilder()
          .setId(s"magazine_$id")
          .setPayload(Payload.newBuilder().setId(s"magazine_$id").build())
          .build()
    for (i <- ids.indices) {
      when(mockContentCache.get(userReads(i).contentId)).thenReturn(content(i))
    }
    userReads
  }

  test("buildFeed without contentId, all unread, contentAmount = 0") {
    val contentAmount = 0
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)

    val unreadUserReads = makeUserReads(0, 3)

    val expectedIdsOrder = List.empty

    val feed = Feed(unread = unreadUserReads, previewed = List.empty, read = List.empty)

    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val response = feedBuilder.buildFeed(userId, None, contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)

    assert(response.getPageStatistics.getTotalCount == 4)
    assert(response.getPageStatistics.getShownContentCount == 0)
    assert(response.getPageStatistics.getFromContentId.isEmpty)
  }

  test("buildFeed without contentId, all unread, contentAmount = -1") {
    val contentAmount = -1
    val source = ContentSource.ALL
    val userIdSource = UserIdSource(userId, source)

    val unreadUserReads = makeUserReads(0, 3)

    val expectedIdsOrder = List.empty

    val feed = Feed(unread = unreadUserReads, previewed = List.empty, read = List.empty)

    when(mockFeedCache.get(userIdSource)).thenReturn(feed)

    val response = feedBuilder.buildFeed(userId, None, contentAmount, ContentSource.ALL).futureValue

    assert(response.getPayloadsList.size() == expectedIdsOrder.size)

    assert(response.getPageStatistics.getTotalCount == 4)
    assert(response.getPageStatistics.getShownContentCount == 0)
    assert(response.getPageStatistics.getFromContentId.isEmpty)
  }
}
