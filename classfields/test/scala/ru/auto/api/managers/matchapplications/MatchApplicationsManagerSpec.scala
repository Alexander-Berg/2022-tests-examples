package ru.auto.api.managers.matchapplications

import java.time.{LocalDate, OffsetDateTime}
import java.util.UUID
import ru.auto.api.model.ModelUtils._
import com.google.protobuf.Timestamp
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.OfferCountResponse
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.managers.searcher.SearchRequestContext
import ru.auto.api.match_applications.MatchApplicationsResponseModel
import ru.auto.api.match_applications.MatchApplicationsResponseModel.MatchApplication.BillingStatus
import ru.auto.api.match_applications.MatchApplicationsResponseModel.MatchApplicationsResponse
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.salesman.match_applications.{MatchApplicationRequest, MatchApplicationRequestsListing}
import ru.auto.api.model.salesman.{Page, Paging}
import ru.auto.api.model.searcher.SearcherRequest
import ru.auto.api.model.{AutoruDealer, CategorySelector}
import ru.auto.api.search.SearchModel.{CatalogFilter, SearchRequestParameters}
import ru.auto.api.services.matchmaker.MatchMakerClient
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.testkit.TestData
import ru.auto.api.ui.UiModel.StateGroup
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.{BaseSpec, BreadcrumbsModel}
import ru.auto.catalog.model.api.ApiModel.{MarkCard, ModelCard, RawCatalog}
import ru.auto.match_maker.model.api.ApiModel._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.paging.Slice

import scala.jdk.CollectionConverters._

class MatchApplicationsManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with BeforeAndAfter {
  private val salesmanClient = mock[SalesmanClient]
  private val matchMakerClient = mock[MatchMakerClient]
  private val catalogManager = mock[CatalogManager]
  private val passportManager = mock[PassportManager]
  private val featureManager = mock[FeatureManager]
  private val searcherClient = mock[SearcherClient]

  private val manager = new MatchApplicationsManager(
    salesmanClient,
    matchMakerClient,
    catalogManager,
    passportManager,
    featureManager,
    searcherClient,
    TestData.tree
  )

  implicit private val trace: RequestImpl = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r
  }

  before {
    when(featureManager.matchApplicationShowConfig).thenReturn(new Feature[MatchApplicationShowConfig] {
      override def name: String = "matchApplicationShowConfig"
      override def value: MatchApplicationShowConfig = MatchApplicationShowConfig.default
    })
  }

  after {
    reset(featureManager)
  }

  "MatchApplicationsManager" should {
    "get match applications for dealer" in {
      val from = LocalDate.of(2020, 1, 1)
      val to = LocalDate.of(2020, 3, 1)
      val pageNum = 1
      val pageSize = 10
      val dealer = AutoruDealer(20101)

      val matchApplicationId1 = UUID.randomUUID().toString

      val salesmanResponse = MatchApplicationRequestsListing(
        Paging(
          1,
          1,
          Page(pageNum, pageSize)
        ),
        List(
          MatchApplicationRequest(20101, "1234-567", matchApplicationId1, OffsetDateTime.now(), "PAID", 100500)
        ),
        100500
      )

      val creationDate = Timestamp.newBuilder().setSeconds(OffsetDateTime.now().toEpochSecond).build()
      val expireDate = creationDate.toBuilder.setSeconds(creationDate.toBuilder.getSeconds + 100).build()

      val searchParams = SearchRequestParameters
        .newBuilder()
        .addCatalogFilter(CatalogFilter.newBuilder().setMark("BMW").setModel("X6").build())
        .build()

      val userInfo = UserInfo
        .newBuilder()
        .setName("Alex")
        .setCreditInfo(CreditInfo.newBuilder().setIsPossible(true))
        .build()

      val matchMakerResponse = MatchApplicationList
        .newBuilder()
        .addMatchApplication(
          MatchApplication
            .newBuilder()
            .setCreationDate(creationDate)
            .setExpireDate(expireDate)
            .setId(matchApplicationId1)
            .setUserInfo(userInfo)
            .setUserProposal(
              UserProposal
                .newBuilder()
                .setSearchParams(searchParams)
            )
        )
        .build()

      when(salesmanClient.getMatchApplicationRequests(dealer, from, to, pageSize, pageNum))
        .thenReturnF(salesmanResponse)

      when(matchMakerClient.getMatchApplications(eq(List(matchApplicationId1)))(any[Request]()))
        .thenReturnF(matchMakerResponse)

      when(
        catalogManager
          .exactByCatalogFilter(
            CategorySelector.Cars,
            subcategory = None,
            searchParams.getCatalogFilterList.asScala.toSeq,
            failNever = false,
            legacyMode = true
          )
      ).thenReturnF(
        RawCatalog
          .newBuilder()
          .putMark(
            "BMW",
            MarkCard
              .newBuilder()
              .setEntity(
                BreadcrumbsModel.Entity
                  .newBuilder()
                  .setName("БНВ")
              )
              .putModel(
                "X6",
                ModelCard.newBuilder().setEntity(BreadcrumbsModel.Entity.newBuilder().setName("ХЭ1")).build()
              )
              .build()
          )
          .build()
      )

      val result = manager
        .getMatchApplications(dealer, MatchApplicationsParams(from, to, pageNum, pageSize))
        .futureValue

      result shouldBe MatchApplicationsResponse
        .newBuilder()
        .setTotalCost(1005)
        .setPaging(
          ru.yandex.vertis.paging.Paging
            .newBuilder()
            .setTotal(1)
            .setPageCount(1)
            .setPage(
              Slice.Page
                .newBuilder()
                .setSize(10)
                .setNum(1)
            )
        )
        .addMatchApplications(
          MatchApplicationsResponseModel.MatchApplication
            .newBuilder()
            .setCreateDate(creationDate)
            .setExpireDate(expireDate)
            .setPrice(1005)
            .setUserInfo(userInfo)
            .setBillingStatus(BillingStatus.PAID)
            .setId(matchApplicationId1)
            .setUserProposal(
              MatchApplicationsResponseModel.MatchApplication.UserProposal
                .newBuilder()
                .setMark("БНВ")
                .setModel("ХЭ1")
            )
        )
        .build()

    }

    "not add match_applications_form config to non-cars offer" in {
      val offer = DealerNonCarsNewOfferGen.next.updated(_.getSellerBuilder.getLocationBuilder.setGeobaseId(1))
      val contexts = manager.offerSearchContexts(offer)
      contexts should be(empty)
    }

    "add match_applications_form config to cars offer" in {
      val offer = DealerCarsNewOfferGen.next.updated(_.getSellerBuilder.getLocationBuilder.setGeobaseId(1))
      val contexts = manager.offerSearchContexts(offer)
      contexts should be(defined)
    }

    "get single match application with redirect phone" in {

      val matchApplicationId = UUID.randomUUID().toString
      val dealerId = 20101L

      val creationDate = Timestamp.newBuilder().setSeconds(OffsetDateTime.now().toEpochSecond).build()
      val expireDate = creationDate.toBuilder.setSeconds(creationDate.toBuilder.getSeconds + 100).build()

      val searchParams = SearchRequestParameters
        .newBuilder()
        .addCatalogFilter(CatalogFilter.newBuilder().setMark("BMW").setModel("X6").build())
        .build()

      val userInfo = UserInfo
        .newBuilder()
        .setName("Alex")
        .setCreditInfo(CreditInfo.newBuilder().setIsPossible(true))
        .build()

      val matchMakerResponse = MatchApplicationList
        .newBuilder()
        .addMatchApplication(
          MatchApplication
            .newBuilder()
            .setCreationDate(creationDate)
            .setExpireDate(expireDate)
            .setId(matchApplicationId)
            .setUserInfo(userInfo)
            .setUserProposal(
              UserProposal
                .newBuilder()
                .setSearchParams(searchParams)
            )
        )
        .build()

      when(matchMakerClient.getMatchApplications(eq(List(matchApplicationId)))(any[Request]()))
        .thenReturnF(matchMakerResponse)

      when(matchMakerClient.getRedirectPhone(eq(matchApplicationId), eq(Some(dealerId)))(any[Request]()))
        .thenReturnF(
          RedirectPhoneInfo
            .newBuilder()
            .setRedirectPhone("88005553535")
            .build()
        )

      when(
        catalogManager
          .exactByCatalogFilter(
            CategorySelector.Cars,
            subcategory = None,
            searchParams.getCatalogFilterList.asScala.toSeq,
            failNever = false,
            legacyMode = true
          )
      ).thenReturnF(
        RawCatalog
          .newBuilder()
          .putMark(
            "BMW",
            MarkCard
              .newBuilder()
              .setEntity(
                BreadcrumbsModel.Entity
                  .newBuilder()
                  .setName("БНВ")
              )
              .putModel(
                "X6",
                ModelCard.newBuilder().setEntity(BreadcrumbsModel.Entity.newBuilder().setName("ХЭ1")).build()
              )
              .build()
          )
          .build()
      )

      val result = manager
        .getMatchApplication(matchApplicationId, dealerId)
        .futureValue

      result shouldBe
        MatchApplicationsResponseModel.MatchApplication
          .newBuilder()
          .setCreateDate(creationDate)
          .setExpireDate(expireDate)
          .setUserInfo(userInfo.toBuilder.setPhone("88005553535").build())
          .setId(matchApplicationId)
          .setUserProposal(
            MatchApplicationsResponseModel.MatchApplication.UserProposal
              .newBuilder()
              .setMark("БНВ")
              .setModel("ХЭ1")
          )
          .build()
    }

    testCanShowForm(expect = true, expectSearcherCall = true)
    testCanShowForm(expect = false, rids = List(225))
    testCanShowForm(expect = false, stateGroup = StateGroup.ALL)
    testCanShowForm(expect = false, contextGen = Gen.const(SearchRequestContext.Type.PremiumNewCars))
    testCanShowForm(expect = false, returnCount = 0)
    testCanShowForm(expect = true, searcherOff = true, returnCount = 0)

    def testCanShowForm(expect: Boolean,
                        rids: List[Integer] = List(1),
                        stateGroup: StateGroup = StateGroup.NEW,
                        contextGen: Gen[SearchRequestContext.ContextType] =
                          Gen.oneOf(Seq(SearchRequestContext.Type.Listing, SearchRequestContext.Type.GroupCard)),
                        returnCount: Int = 2,
                        expectSearcherCall: Boolean = false,
                        searcherOff: Boolean = false): Unit = {
      val context = contextGen.next
      s"${if (expect) "can" else "can't"} show match application form with ($rids, $stateGroup, $context, searcher ${if (searcherOff) "off" else "on"})" in {
        val params = SearchRequestParameters
          .newBuilder()
          .setStateGroup(stateGroup)
          .addAllRid(rids.asJava)
          .build()
        val searcherRequest = SearcherRequest(Cars, Map.empty[String, Set[String]])

        when(searcherClient.offersCount(?, ?)(?))
          .thenReturnF(OfferCountResponse.newBuilder().setCount(returnCount).build())
        when(featureManager.matchApplicationNoSearch).thenReturn(new Feature[Boolean] {
          override def name: String = "match_application_no_search"

          override def value: Boolean = searcherOff
        })

        manager.checkIfCanShowMatchApplicationForm(params, searcherRequest, context).futureValue shouldBe expect

        if (expectSearcherCall) {
          verify(searcherClient).offersCount(?, ?)(?)
        }
      }
    }
  }
}
