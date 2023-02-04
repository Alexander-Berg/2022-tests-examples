package vsmoney.auction_auto_strategy.scheduler.test

import common.autoru.clients.public_api.model.SearchGroupBy
import common.autoru.clients.public_api.testkit.PublicApiClientMock
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ResponseModel.{OfferListingResponse, Pagination}
import ru.auto.api.search.SearchModel
import ru.auto.api.search.SearchModel.{CatalogFilter, SearchRequestParameters}
import vsmoney.auction_auto_strategy.model.auction.{
  AuctionKey,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue
}
import vsmoney.auction_auto_strategy.model.common.{ProductId, Project, UserId}
import vsmoney.auction_auto_strategy.scheduler.service.ActiveUserService
import vsmoney.auction_auto_strategy.scheduler.service.impl.AutoruActiveUserService
import zio.ZIO
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._
import zio.test.mock.Expectation._

import scala.jdk.CollectionConverters._

object AutoruActiveUserServiceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AutoruActiveUserService forAuction")(
      testM("should collect user with offers from public api") {

        val offers = List(
          Offer.newBuilder().setUserRef(user1.id).build(),
          Offer.newBuilder().setUserRef(user2.id).build()
        )
        val listing = OfferListingResponse.newBuilder().addAllOffers(offers.asJava).build()
        val expected = Set(user1, user2)

        val env = PublicApiClientMock.SearchCars(anything, value(listing)) >>> AutoruActiveUserService.live

        val result = (for {
          service <- ZIO.service[ActiveUserService]
          res <- service.forAuction(testAuction)
        } yield res).provideCustomLayer(env)

        assertM(result)(equalTo(expected))
      },
      testM("should collect user with offers from all pages from public api") {

        val listingPage1 = OfferListingResponse
          .newBuilder()
          .addOffers(Offer.newBuilder().setUserRef(user1.id))
          .setPagination(Pagination.newBuilder().setTotalPageCount(2).setPage(1).setPageSize(1).setTotalOffersCount(2))
          .build()

        val listingPage2 = OfferListingResponse
          .newBuilder()
          .addOffers(Offer.newBuilder().setUserRef(user2.id))
          .setPagination(Pagination.newBuilder().setTotalPageCount(2).setPage(2).setPageSize(1).setTotalOffersCount(2))
          .build()

        val expected = Set(user1, user2)

        val env = (PublicApiClientMock
          .SearchCars(equalTo((testSearchParams, 1, 100, SearchGroupBy.DealerId)), value(listingPage1)) ++
          PublicApiClientMock
            .SearchCars(equalTo((testSearchParams, 2, 100, SearchGroupBy.DealerId)), value(listingPage2))) >>>
          AutoruActiveUserService.live

        val result = (for {
          service <- ZIO.service[ActiveUserService]
          res <- service.forAuction(testAuction)
        } yield res).provideCustomLayer(env)

        assertM(result)(equalTo(expected))

      }
    )
  }

  private val product = ProductId("call")
  private val project = Project.Autoru
  private val user1 = UserId("user:1")
  private val user2 = UserId("user:1")

  private val testRegion = 1
  private val testMark = "BMW"
  private val testModel = "X3"

  private val testContext = CriteriaContext(
    List(
      Criterion(CriterionKey("region_id"), CriterionValue(testRegion.toString)),
      Criterion(CriterionKey("mark"), CriterionValue(testMark)),
      Criterion(CriterionKey("model"), CriterionValue(testModel))
    )
  )

  private val testAuction =
    AuctionKey(
      project,
      product,
      testContext
    )

  private val testSearchParams = SearchRequestParameters
    .newBuilder()
    .addRid(testRegion)
    .addState(SearchModel.State.NEW)
    .addCatalogFilter(
      CatalogFilter.newBuilder().setMark(testMark).setModel(testModel).build()
    )
    .build()

}
