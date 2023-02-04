package ru.auto.api.services.review

import java.lang.System.currentTimeMillis

import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.ModelGenerators.PersonalUserRefGen
import ru.auto.api.model._
import ru.auto.api.model.magazine.ArticlesFilter
import ru.auto.api.model.reviews.ReviewModelGenerators.ReviewGen
import ru.auto.api.reviews.ReviewModel.Review
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.tracing.Traced

class DefaultReviewsSearcherClientTest extends HttpClientSuite with Matchers with ScalaFutures {

  override protected def config: HttpClientConfig = {
    HttpClientConfig("reviews-api-main.vrts-slb.test.vertis.yandex.net", 80)
  }

  val client = new DefaultReviewsSearcherClient(http)
  val review: Review = ReviewGen.next

  def changeReview(f: Review.Builder => Unit): Review = {
    val builder = review.toBuilder
    f(builder)
    builder.build()
  }

  val changedUserRef = UserRef.parse("user:111")
  val reviewChangedAuto: Review = changeReview(_.getItemBuilder.getAutoBuilder.setMark("VAZ"))
  val reviewChangedUser: Review = changeReview(_.getReviewerBuilder.setId(changedUserRef.toPlain))
  val user: UserRef = UserRef.parse(review.getReviewer.getId)

  val articlesFilter = ArticlesFilter.apply(categories = Seq("auto"))

  val sorting = SortingByField("publishDate", true)
  val paging: Paging = Paging.Default
  val excludeOfferId: Option[String] = None
  var id: String = "0"
  var idChangedAuto = "2"
  var idChangedUser = "3"
  val pro = "pro"
  val startTime = currentTimeMillis()

  implicit override val trace: Traced = Traced.empty

  implicit private val request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
    r.setUser(PersonalUserRefGen.next)
    r.setTrace(trace)
    r
  }
  test("not return unexisting review anywhere") {
    pending
    client.getReview(id).failed.futureValue should not be null
  }

  test("pagination should work") {
    pending
    client
      .getArticlesListing(articlesFilter, paging, sorting)
      .futureValue should not be null
  }
}
