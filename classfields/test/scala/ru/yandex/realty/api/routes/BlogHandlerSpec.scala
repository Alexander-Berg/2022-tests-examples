package ru.yandex.realty.api.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.api.ProtoResponse.BlogPostsResponse
import ru.yandex.realty.blogs.BlogPosts
import ru.yandex.realty.controllers.blog.BlogsRequest
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.blog.BlogManager
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.user.UserRefGenerators
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * Specs on [[BlogHandler]]
  *
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class BlogHandlerSpec extends HandlerSpecBase with UserRefGenerators with PropertyChecks {

  private val manager: BlogManager = mock[BlogManager]

  override def routeUnderTest: Route = new BlogHandler(manager).route

  "GET /blog/posts" should {

    "return some posts for positive size parameter" in {

      val size = 42
      val request = Get(s"/blog/posts?size=$size")
      val correctResponse = BlogPostsResponse.getDefaultInstance

      forAll(passportUserGen) { user =>
        (manager
          .getBlogPosts(_: BlogsRequest)(_: Traced))
          .expects(where { (blogsRequest, _) =>
            blogsRequest.size == size &&
            blogsRequest.`type`.isEmpty &&
            blogsRequest.category.isEmpty &&
            blogsRequest.newFlat.isEmpty
          })
          .returning(Future.successful(BlogPostsResponse.getDefaultInstance))

        request.withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            entityAs[BlogPostsResponse] should be(correctResponse)
          }
      }
    }

    "return some posts for full parameter list" in {

      val size = 42
      val `type` = OfferType.SELL
      val category = CategoryType.APARTMENT
      val newFlat = "YES"
      val request = Get(s"/blog/posts?size=$size&type=${`type`}&category=$category&newFlat=$newFlat")
      val correctResponse = BlogPostsResponse.getDefaultInstance

      forAll(passportUserGen) { user =>
        (manager
          .getBlogPosts(_: BlogsRequest)(_: Traced))
          .expects(where { (blogsRequest, _) =>
            blogsRequest.size == size &&
            blogsRequest.`type`.contains(`type`) &&
            blogsRequest.category.contains(category) &&
            blogsRequest.newFlat.contains(true)
          })
          .returning(Future.successful(BlogPostsResponse.getDefaultInstance))

        request.withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            entityAs[BlogPostsResponse] should be(correctResponse)
          }
      }
    }

    "fail if size is not set" in {

      val badRequest = Get(s"/blog/posts")

      forAll(passportUserGen) { user =>
        badRequest.withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.BadRequest)
          }
      }
    }

    "fail if no authorization provided" in {

      val noAuthRequest = Get(s"/blog/posts?size=4")

      noAuthRequest ~>
        route ~>
        check {
          status should be(StatusCodes.Unauthorized)
        }
    }
  }

  override protected val exceptionHandler: ExceptionHandler = defaultExceptionHandler

  override protected val rejectionHandler: RejectionHandler = defaultRejectionHandler
}
