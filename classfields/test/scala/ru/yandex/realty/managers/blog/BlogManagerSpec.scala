package ru.yandex.realty.managers.blog

import org.joda.time.Instant
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.ProtoResponse.BlogPostsResponse
import ru.yandex.realty.clients.blogs.BlogsClient
import ru.yandex.realty.clients.blogs.BlogsClient.{Post, PostBody, PostTag}
import ru.yandex.realty.controllers.blog.BlogsRequest
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * Specs on [[BlogManager]]
  *
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class BlogManagerSpec extends AsyncSpecBase {

  private val blogsClient: BlogsClient = mock[BlogsClient]

  private val allPosts = List(
    Post(
      id = "2",
      publishDate = Instant.now,
      approvedTitle = "at",
      approvedBody = PostBody("", None, None),
      approvedPreview = PostBody("", None, None),
      titleImage = Map.empty,
      tags = List(PostTag("дизайн", "дизайн")),
      authorId = "bbb",
      slug = "s-2",
      viewType = "vt",
      commentsCount = 1,
      hasNext = false
    ),
    Post(
      id = "1",
      publishDate = Instant.now,
      approvedTitle = "at",
      approvedBody = PostBody("", None, None),
      approvedPreview = PostBody("", None, None),
      titleImage = Map.empty,
      tags = List(PostTag("аренда", "аренда")),
      authorId = "aaa",
      slug = "s-1",
      viewType = "vt",
      commentsCount = 1,
      hasNext = false
    ),
    Post(
      id = "3",
      publishDate = Instant.now,
      approvedTitle = "at",
      approvedBody = PostBody("", None, None),
      approvedPreview = PostBody("", None, None),
      titleImage = Map.empty,
      tags = List(PostTag("вторичное жильё", "вторичное жильё")),
      authorId = "aaa",
      slug = "s-3",
      viewType = "vt",
      commentsCount = 1,
      hasNext = false
    ),
    Post(
      id = "4",
      publishDate = Instant.now,
      approvedTitle = "at",
      approvedBody = PostBody("", None, None),
      approvedPreview = PostBody("", None, None),
      titleImage = Map.empty,
      tags = List(PostTag("новостройки", "новостройки")),
      authorId = "aaa",
      slug = "s-1",
      viewType = "vt",
      commentsCount = 1,
      hasNext = false
    )
  )

  trait BlogManagerBuilder {

    val blogManager =
      new BlogManager(blogsClient, "https://realty.test.vertis.yandex.ru")
  }

  "BlogManager" should {
    "return articles with tag rent if there are type=RENT and category=APARTMENT in query" in new BlogManagerBuilder {
      val req = BlogsRequest(
        userRef = UserRef.empty,
        `type` = Some(OfferType.RENT),
        category = Some(CategoryType.APARTMENT),
        newFlat = None,
        size = 2
      )

      expectGetPosts()

      val result: BlogPostsResponse = blogManager.getBlogPosts(req)(Traced.empty).futureValue
      result.getResponse.getPostsList.asScala.map(_.getId) should be(List("1", "2"))
    }

    "return articles with tag secondary for type=SELL and category=APARTMENT and newFlat=false in query" in
      new BlogManagerBuilder {

        val req = BlogsRequest(
          userRef = UserRef.empty,
          `type` = Some(OfferType.SELL),
          category = Some(CategoryType.APARTMENT),
          newFlat = Some(false),
          size = 2
        )

        expectGetPosts()

        val result: BlogPostsResponse = blogManager.getBlogPosts(req)(Traced.empty).futureValue
        result.getResponse.getPostsList.asScala.map(_.getId) should be(List("3", "2"))
      }

    "return articles with tag secondary for type=SELL and category=APARTMENT and newFlat=true in query" in
      new BlogManagerBuilder {

        val req = BlogsRequest(
          userRef = UserRef.empty,
          `type` = Some(OfferType.SELL),
          category = Some(CategoryType.APARTMENT),
          newFlat = Some(true),
          size = 2
        )

        expectGetPosts()

        val result: BlogPostsResponse = blogManager.getBlogPosts(req)(Traced.empty).futureValue
        result.getResponse.getPostsList.asScala.map(_.getId) should be(List("4", "2"))
      }
  }

  private def expectGetPosts(): Unit = {
    (blogsClient
      .getPosts(_: String, _: Option[String], _: Option[Int], _: Option[String], _: Option[String])(_: Traced))
      .expects(BlogsClient.BlogId.Realty, *, *, *, *, *)
      .returning(Future.successful(allPosts))
  }
}
