package ru.auto.api.routes.v1.dealer

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, OffsetDateTime}
import java.util
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `application/octet-stream`}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import com.google.protobuf.{BoolValue, Int32Value}
import org.apache.commons.io.IOUtils
import org.mockito.Mockito._
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Section
import ru.auto.api.ApiSpec
import ru.auto.api.CampaignModel.Campaigns
import ru.auto.api.ResponseModel.ExpensesReportResponse.ProcessStatus
import ru.auto.api.ResponseModel._
import ru.auto.api.StatisticsListingModel.{DealerDailyBalanceStatsListing, OfferProductsActivationsStatsListing}
import ru.auto.api.StatisticsModel.DealerBalanceDailyStat
import ru.auto.api.TradeInRequestsListingOuterClass.TradeInRequestsListing
import ru.auto.api.billing.LightResponseModel.CampaignResponseListing
import ru.auto.api.dealer.DealerInfoModel
import ru.auto.api.dealer.proto.DealerPonyModel.{PhonesResultList, SimplePhonesList}
import ru.auto.api.exceptions.{TeleponyAccessException, TeleponyInvalidRequest, TeleponyRecordNotFound, TeleponyRecordNotReady}
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.carfax.CarfaxManager
import ru.auto.api.managers.counters.CountersManager
import ru.auto.api.managers.dealer.{AvitoWalletOperationTotalStatsFilter, DealerStatsManager, DealerTariffManager}
import ru.auto.api.managers.redemption.RedemptionManager
import ru.auto.api.managers.salesman.CampaignManager
import ru.auto.api.managers.tradein.TradeInManager
import ru.auto.api.managers.wallet.{OfferSorting, WalletListingParams, WalletManager}
import ru.auto.api.model.AutoruProduct.Placement
import ru.auto.api.model.DealerGenerators._
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model._
import ru.auto.api.model.gen.CountersModelGenerators.dealerDailyCountersResponse
import ru.auto.api.model.gen.DeprecatedBillingModelGenerators._
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.model.salesman.convertCampaigns
import ru.auto.api.multiposting.MulipostingResponseModel.AvitoWalletDailyOperationListingResponse
import ru.auto.api.services.MockedClients
import ru.auto.api.services.cabinet.{CheckAccessView, ClientPropertiesView, ClientView}
import ru.auto.api.services.telepony.TeleponyClient
import ru.auto.api.services.telepony.TeleponyClient.CallRecord
import ru.auto.api.util.{ManagerUtils, Resources}
import ru.auto.cabinet.AclResponse.{AccessGrants, AccessLevel, GroupsList, ResourceAlias}
import ru.auto.cabinet.ApiModel.{DealerRedirect, GetDealerRedirectsResult}
import ru.auto.cabinet.DealerResponse.DealerUser
import ru.auto.cabinet.Redemption.RedemptionForm
import ru.auto.cabinet.Redemption.RedemptionForm.{ClientInfo, DealerInfo}
import ru.auto.catalog.model.api.ApiModel.{MarkCard, ModelCard, RawCatalog}
import ru.auto.dealer_pony.proto.ApiModel.{AddPhoneNumbersRequest, CallInfoResponse, DeletePhoneNumbersRequest}
import ru.auto.dealer_stats.proto.Rpc.DealerWarehouseIndicatorsResponse
import ru.auto.multiposting.WalletService.{GetAvitoTariffResponse, GetAvitoWalletBalanceResponse}
import ru.yandex.passport.model.api.ApiModel._
import ru.yandex.vertis.feature.model.Feature
import vsmoney.auction.AuctionBidsOuterClass.AuctionStates
import vsmoney.auction_auto_strategy.Settings.AutoStrategyListResponse

import scala.concurrent.Future

/**
  * Тестируем [[DealerHandler]].
  */
class DealerHandlerTest extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  override lazy val walletManager: WalletManager = mock[WalletManager]
  override lazy val dealerTariffManager: DealerTariffManager = mock[DealerTariffManager]
  override lazy val dealerStatsManager: DealerStatsManager = mock[DealerStatsManager]
  override lazy val redemptionManager: RedemptionManager = mock[RedemptionManager]
  override lazy val tradeInManager: TradeInManager = mock[TradeInManager]
  override lazy val countersManager: CountersManager = mock[CountersManager]
  override lazy val campaignManager: CampaignManager = mock[CampaignManager]
  override lazy val carfaxManager: CarfaxManager = mock[CarfaxManager]
  override lazy val featureManager: FeatureManager = mock[FeatureManager]

  private val checkAccessClientView = CheckAccessView(role = DealerUserRoles.Client)

  before {
    reset(campaignManager)
  }

  when(featureManager.enrichDealerSessionWithGroup).thenReturn {
    new Feature[Boolean] {
      override def name: String = "enrich_dealer_session_with_group"
      override def value: Boolean = true
    }
  }

  when(featureManager.hidePricesForDealers).thenReturn {
    new Feature[Boolean] {
      override def name: String = "hide_prices_for_dealers"
      override def value: Boolean = true
    }
  }

  def sameElements[A](expected: List[A]): List[A] = {
    ArgumentMatchers.argThat[List[A]](new ArgumentMatcher[List[A]] {
      override def matches(actual: List[A]): Boolean = {
        actual.toSet == expected.toSet
      }
    })
  }

  "/dealer/trade-in" should {

    "return records" in {
      forAll(DealerUserRefGen, SessionResultGen) { (dealer, session) =>
        reset(tradeInManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.TRADE_IN, AccessLevel.READ_ONLY).next
        }
        when(tradeInManager.getTradeInRequests(?, ?, ?, ?, ?)(?)).thenReturnF(
          TradeInRequestsListing.newBuilder().build()
        )
        Get(s"/1.0/dealer/trade-in?section=NEW&from_date=2019-01-01&to_date=2019-02-02&page=1&page_size=10") ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/octet-stream`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          verify(tradeInManager).getTradeInRequests(
            eq(dealer.clientId),
            eq(Some(Section.NEW)),
            eq(LocalDate.of(2019, 1, 1)),
            eq(Some(LocalDate.of(2019, 2, 2))),
            eq(Paging(1, 10))
          )(?)
          verifyNoMoreInteractions(tradeInManager)
        }
      }
    }

    "return bad request if no from_date required" in {
      forAll(DealerUserRefGen, SessionResultGen) { (dealer, session) =>
        reset(tradeInManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)
        when(tradeInManager.getTradeInRequests(?, ?, ?, ?, ?)(?)).thenReturnF(
          TradeInRequestsListing.newBuilder().build()
        )
        Get(s"/1.0/dealer/trade-in?section=NEW&to_date=2019-02-02&page_size=10&page_num=2") ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/octet-stream`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~> check {
          status shouldBe StatusCodes.BadRequest
          verifyNoMoreInteractions(tradeInManager)
        }
      }
    }
  }

  "/dealer/wallet/daily-balance-stats" should {

    "return dealer daily balance stats listing" in {
      forAll(DealerUserRefGen, SessionResultGen) { (dealer, session) =>
        reset(walletManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
        }
        when(walletManager.getDealerBalanceDailyStats(?, ?)(?)).thenReturnF(
          DealerDailyBalanceStatsListing
            .newBuilder()
            .addAllStats(
              util.Arrays.asList(
                DealerBalanceDailyStat
                  .newBuilder()
                  .setDate("2019-01-01")
                  .setBalance(2030)
                  .build()
              )
            )
            .build()
        )
        Get(s"/1.0/dealer/wallet/daily-balance-stats?product=parts-placement&from=2019-01-01&to=2019-02-02") ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/octet-stream`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          verify(walletManager).getDealerBalanceDailyStats(
            eq(dealer),
            eq(WalletListingParams(LocalDate.of(2019, 1, 1), Some(LocalDate.of(2019, 2, 2)), None, None))
          )(?)
          verifyNoMoreInteractions(walletManager)
        }
      }
    }
  }

  "/dealer/info" should {
    "shall work fine" in {
      forAll(DealerUserRefGen, SessionResultGen) { (dealer, session) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_ONLY).next
        }

        val response = DealerInfoModel.DealerInfo.newBuilder().setMultipostingEnabled(true).build()
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        Get(s"/1.0/dealer/info") ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[DealerInfoModel.DealerInfo] shouldBe response
          }
      }
    }
  }

  "/dealer/tariff" should {
    "shall work fine and get tariffs with discount" in {
      forAll(DealerUserRefGen, SessionResultGen, availableTariffsResponseGen) { (dealer, session, tariffs) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_ONLY).next
        }
        when(dealerTariffManager.tariffsForDealer(?)(?)).thenReturnF(tariffs)

        Get(s"/1.0/dealer/tariff") ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[AvailableTariffsResponse] shouldBe tariffs
          }
      }
    }
  }

  "/dealer/account" should {

    "return 401 if it is not a dealer session" in {

      forAll(AnonSessionResultGen) { session =>
        when(passportClient.createAnonymousSession()(?)).thenReturnF(session)
        when(passportClient.getSession(?)(?)).thenReturnF(session)

        Get("/1.0/dealer/account") ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe StatusCodes.Unauthorized
            }
          }
      }
    }
  }

  "GET /dealer/expenses-report" should {
    "work fine" in {

      val fromDate = LocalDate.now().minusYears(1)
      val toDate = LocalDate.now().minusDays(1)
      val processStatus = ProcessStatus.WILL_BE_SENT_TO_EMAIL

      forAll(DealerUserRefGen, SessionResultGen) { (dealer, session) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
        }
        when(cabinetApiClient.expensesReport(?, ?, ?, ?)(?))
          .thenReturnF(ExpensesReportResponse.newBuilder().setProcessStatus(processStatus).build())

        Get(
          s"/1.0/dealer/expenses-report?from_date=${fromDate.format(DateTimeFormatter.ISO_DATE)}" +
            s"&to_date=${toDate.format(DateTimeFormatter.ISO_DATE)}"
        ) ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            val response = responseAs[ExpensesReportResponse]
            response.getProcessStatus shouldBe processStatus
          }
      }
    }
  }

  "GET /dealer/call-center/redirects" should {
    "call cabinetApiClient.getDealerRedirects with proper mark and model" in {
      forAll(CarInfoGen) { carInfo =>
        val result = GetDealerRedirectsResult
          .newBuilder()
          .addAllDealerRedirects(
            util.Arrays.asList(DealerRedirect.newBuilder().setMark(carInfo.getMark).setModel(carInfo.getModel).build())
          )
          .build()

        when(cabinetApiClient.getDealerRedirects(eq(carInfo.getMark), eq(carInfo.getModel))(?))
          .thenReturn(Future.successful(result))

        Get(
          s"/1.0/dealer/call-center/redirects?mark=${carInfo.getMark}&model=${carInfo.getModel}"
        ) ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/json`)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[GetDealerRedirectsResult] shouldBe result
          }
      }
    }
  }

  "POST /dealer/{id}/redemption" should {

    val userName = "Vasiya"
    val telNumber = "+71234567890"
    val dealerProfileUri = "http://hello-world.ru/123"
    val offerId = "12345"

    def testRedemptionForm(): RedemptionForm = {
      RedemptionForm
        .newBuilder()
        .setClient(
          ClientInfo
            .newBuilder()
            .setName(userName)
            .setPhoneNumber(telNumber)
        )
        .setDealerInfo(
          DealerInfo
            .newBuilder()
            .setProfileUrl(dealerProfileUri)
        )
        .setDesiredOfferId(offerId)
        .build()
    }

    "work fine when data is correct and request is anonymous" in {
      forAll(DealerUserRefGen) { dealer =>
        reset(cabinetApiClient, redemptionManager)
        when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
        when(redemptionManager.createRedemption(?, ?)(?)).thenReturnF(ManagerUtils.SuccessResponse)

        Post(s"/1.0/dealer/${dealer.clientId}/redemption", testRedemptionForm()) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            verify(redemptionManager).createRedemption(?, ?)(?)
          }
      }
    }

    "return 500 if exception is thrown" in {
      forAll(DealerUserRefGen) { dealer =>
        reset(cabinetApiClient, redemptionManager)
        when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
        when(redemptionManager.createRedemption(?, ?)(?)).thenReturn(Future.failed(new IllegalStateException()))

        Post(s"/1.0/dealer/${dealer.clientId}/redemption", testRedemptionForm()) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            status shouldBe StatusCodes.InternalServerError
            verify(redemptionManager).createRedemption(?, ?)(?)
          }
      }
    }
  }

  "/dealer/campaigns" should {

    "get campaigns" in {
      forAll(DealerUserRefGen, SessionResultGen, Gen.listOf(singleCarsNewCampaignGen).map(_.toSet)) {
        (dealer, session, campaigns) =>
          reset(passportClient, cabinetApiClient, campaignManager)
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
          when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)
          when(campaignManager.getCampaigns(?)(?)).thenReturnF(convertCampaigns(campaigns))

          Get("/1.0/dealer/campaigns") ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("X-Dealer-ID", dealer.clientId.toString) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              withClue(responseAs[String]) {
                status shouldBe StatusCodes.OK
                val response = responseAs[Campaigns]
                response shouldBe convertCampaigns(campaigns)

                verify(campaignManager).getCampaigns(eq(dealer))(?)
              }
            }
      }
    }
  }

  "/dealer/campaign/products" should {
    "get campaigns without products" in {
      forAll(DealerUserRefGen, SessionResultGen, CampaignResponseListingGen) { (dealer, session, campaignsResponse) =>
        reset(campaignManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(campaignManager.getProductCampaignsList(?, ?)(?)).thenReturnF(campaignsResponse)

        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_ONLY).next
        }

        Get("/1.0/dealer/campaign/products") ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("X-Dealer-ID", dealer.clientId.toString) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe StatusCodes.OK
              val response = responseAs[CampaignResponseListing]
              response shouldBe campaignsResponse

              verify(campaignManager).getProductCampaignsList(eq(dealer), eq(Nil))(?)
            }
          }
      }
    }

    "get campaigns with no trade-in products" in {
      forAll(DealerUserRefGen, SessionResultGen, NonTradeInProductGen, CampaignResponseListingGen) {
        (dealer, session, product, campaignsResponse) =>
          reset(campaignManager)
          when(passportClient.getSession(?)(?)).thenReturnF(session)

          when(campaignManager.getProductCampaignsList(?, ?)(?))
            .thenReturnF(campaignsResponse)

          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_ONLY).next
          }

          Get(s"/1.0/dealer/campaign/products?product=${product.name}") ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("X-Dealer-ID", dealer.clientId.toString) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              withClue(responseAs[String]) {
                status shouldBe StatusCodes.OK
                val response = responseAs[CampaignResponseListing]
                response shouldBe campaignsResponse

                verify(campaignManager).getProductCampaignsList(eq(dealer), eq(List(product)))(?)
              }
            }
      }
    }

    "get campaigns with products include trade-in" in {
      forAll(DealerUserRefGen, SessionResultGen, TradeInProductGen, CampaignResponseListingGen) {
        (dealer, session, product, campaignsResponse) =>
          reset(campaignManager)
          when(passportClient.getSession(?)(?)).thenReturnF(session)

          when(campaignManager.getProductCampaignsList(?, ?)(?))
            .thenReturnF(campaignsResponse)

          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.TRADE_IN, AccessLevel.READ_ONLY).next
          }

          Get(s"/1.0/dealer/campaign/products?product=${product.name}") ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("X-Dealer-ID", dealer.clientId.toString) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              withClue(responseAs[String]) {
                status shouldBe StatusCodes.OK
                val response = responseAs[CampaignResponseListing]
                response shouldBe campaignsResponse

                verify(campaignManager).getProductCampaignsList(eq(dealer), eq(List(product)))(?)
              }
            }
      }
    }

    "fail on get campaigns without permission" in {
      forAll(DealerUserRefGen, SessionResultGen, ProductGen, CampaignResponseListingGen) {
        (dealer, session, product, campaignsResponse) =>
          reset(campaignManager)
          when(passportClient.getSession(?)(?)).thenReturnF(session)

          when(campaignManager.getProductCampaignsList(?, ?)(?))
            .thenReturnF(campaignsResponse)

          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.UNKNOWN_RESOURCE, AccessLevel.READ_ONLY).next
          }

          Get(s"/1.0/dealer/campaign/products?product=${product.name}") ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("X-Dealer-ID", dealer.clientId.toString) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              withClue(responseAs[String]) {
                status shouldBe StatusCodes.Forbidden
                verify(campaignManager, never).getProductCampaignsList(eq(dealer), eq(List(product)))(?)
              }
            }
      }
    }

    "get campaigns with products [old aliases]" in {
      forAll(DealerUserRefGen, SessionResultGen, Gen.listOfN(3, NonTradeInProductGen), CampaignResponseListingGen) {
        (dealer, session, products, campaignsResponse) =>
          reset(campaignManager)
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
          when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)

          when(campaignManager.getProductCampaignsList(?, ?)(?))
            .thenReturnF(campaignsResponse)

          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_ONLY).next
          }

          val productNames = products.map(p => s"product=${p.salesName}").mkString("&")

          Get(s"/1.0/dealer/campaign/products?$productNames") ~>
            addHeader(Accept(`application/json`)) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("X-Dealer-ID", dealer.clientId.toString) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              withClue(responseAs[String]) {
                status shouldBe StatusCodes.OK
                val response = responseAs[CampaignResponseListing]
                response shouldBe campaignsResponse

                verify(campaignManager).getProductCampaignsList(
                  eq(dealer),
                  sameElements[AutoruProduct](products)
                )(?)
              }
            }
      }
    }
  }

  "/dealer/wallet/recharges" should {

    "get recharges" in {
      forAll(DealerUserRefGen, SessionResultGen, walletRechargesListingGen) { (dealer, session, recharges) =>
        reset(passportClient, walletManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
        }
        when(walletManager.getRecharges(?)(?)).thenReturnF(recharges)
        Get("/1.0/dealer/wallet/recharges?from=2018-06-01&to=2018-06-19") ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          xAuthorizationHeader ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[WalletRechargesListing] shouldBe recharges
          verify(walletManager).getRecharges(
            eq(
              WalletListingParams(
                LocalDate.of(2018, 6, 1),
                Some(LocalDate.of(2018, 6, 19)),
                pageNum = None,
                pageSize = None
              )
            )
          )(?)
          verifyNoMoreInteractions(walletManager)
        }
      }
    }
  }

  "/dealer/wallet/product/activations/daily-stats" should {

    "get stats" in {
      forAll(DealerUserRefGen, SessionResultGen, productActivationsDailyStatListingGen) { (dealer, session, stats) =>
        reset(passportClient, walletManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
        }
        when(walletManager.getProductActivationsDailyStats(?, ?, ?)(?)).thenReturnF(stats)
        Get(
          "/1.0/dealer/wallet/product/activations/daily-stats?" +
            "from=2018-06-12&to=2018-06-18&pageNum=5&pageSize=10&service=autoru"
        ) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          xAuthorizationHeader ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ProductActivationsDailyStatListing] shouldBe stats
          verify(walletManager).getProductActivationsDailyStats(
            eq(dealer),
            eq(
              WalletListingParams(
                LocalDate.of(2018, 6, 12),
                Some(LocalDate.of(2018, 6, 18)),
                Some(5),
                Some(10)
              )
            ),
            eq(None)
          )(?)
          verifyNoMoreInteractions(walletManager)
        }
      }
    }
  }

  "/dealer/wallet/product/<product>/activations/offer-stats" should {

    "get stats for offers" in {
      forAll(DealerUserRefGen, SessionResultGen, offerProductActivationsDailyStatsListingGen) {
        (dealer, session, stats) =>
          reset(passportClient, cabinetApiClient, walletManager)
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
          }
          when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
          when(walletManager.getGroupedProductActivationsDailyStats(?, ?, ?, ?, ?)(?)).thenReturnF(stats)
          Get(
            "/1.0/dealer/wallet/product/placement/activations/offer-stats?" +
              "date=2018-06-13&pageNum=5&pageSize=10&service=autoru"
          ) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            xAuthorizationHeader ~>
            route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[OfferProductActivationsDailyStatsListing] shouldBe stats
            verify(walletManager).getGroupedProductActivationsDailyStats(
              eq(dealer),
              eq(Placement),
              eq(LocalDate.of(2018, 6, 13)),
              eq(5),
              eq(10)
            )(?)
            verifyNoMoreInteractions(walletManager)
          }
      }
    }
  }

  "/dealer/wallet/product/activations/total-stats" should {

    "get stats" in {
      forAll(DealerUserRefGen, SessionResultGen, productActivationsStatsGen) { (dealer, session, stats) =>
        reset(passportClient, walletManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
        }
        when(walletManager.getProductActivationsStats(?, ?, ?, ?, ?)(?)).thenReturnF(stats)
        Get(
          "/1.0/dealer/wallet/product/activations/total-stats?" +
            "from=2018-06-13&to=2018-07-02&service=autoru"
        ) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          xAuthorizationHeader ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[ProductActivationsStats] shouldBe stats
          verify(walletManager).getProductActivationsStats(
            eq(dealer),
            eq(None),
            eq(None),
            eq(LocalDate.of(2018, 6, 13)),
            eq(Some(LocalDate.of(2018, 7, 2)))
          )(?)
          verifyNoMoreInteractions(walletManager)
        }
      }
    }
  }

  "/dealer/wallet/product/activations/total-offer-stats" should {

    "get stats with implicit sorting by id" in {
      forAll(DealerUserRefGen, SessionResultGen, offerProductsActivationsStatsListingGen) { (dealer, session, stats) =>
        reset(passportClient, walletManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
        }
        when(walletManager.getOfferProductsActivationsTotalStats(?, ?, ?, ?, ?)(?)).thenReturnF(stats)
        Get(
          "/1.0/dealer/wallet/product/activations/total-offer-stats?" +
            "from=2018-06-13&to=2018-07-02&pageNum=3&pageSize=25&service=autoru"
        ) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          xAuthorizationHeader ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[OfferProductsActivationsStatsListing] shouldBe stats
          verify(walletManager).getOfferProductsActivationsTotalStats(
            eq(dealer),
            eq(WalletListingParams(LocalDate.of(2018, 6, 13), Some(LocalDate.of(2018, 7, 2)), Some(3), Some(25))),
            eq(OfferSorting.OfferId),
            eq(None),
            eq(None)
          )(?)
          verifyNoMoreInteractions(walletManager)
        }
      }
    }

    "get stats with explicit sorting by id" in {
      forAll(DealerUserRefGen, SessionResultGen, offerProductsActivationsStatsListingGen) { (dealer, session, stats) =>
        reset(passportClient, walletManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
        }
        when(walletManager.getOfferProductsActivationsTotalStats(?, ?, ?, ?, ?)(?)).thenReturnF(stats)
        Get(
          "/1.0/dealer/wallet/product/activations/total-offer-stats?" +
            "from=2018-06-13&to=2018-07-02&pageNum=3&pageSize=25&service=autoru&sort=offer_id"
        ) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          xAuthorizationHeader ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[OfferProductsActivationsStatsListing] shouldBe stats
          verify(walletManager).getOfferProductsActivationsTotalStats(
            eq(dealer),
            eq(WalletListingParams(LocalDate.of(2018, 6, 13), Some(LocalDate.of(2018, 7, 2)), Some(3), Some(25))),
            eq(OfferSorting.OfferId),
            eq(None),
            eq(None)
          )(?)
          verifyNoMoreInteractions(walletManager)
        }
      }
    }

    "get stats with default sorting (by id)" in {
      forAll(DealerUserRefGen, SessionResultGen, offerProductsActivationsStatsListingGen) { (dealer, session, stats) =>
        reset(passportClient, walletManager)
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
        }
        when(walletManager.getOfferProductsActivationsTotalStats(?, ?, ?, ?, ?)(?)).thenReturnF(stats)
        Get(
          "/1.0/dealer/wallet/product/activations/total-offer-stats?" +
            "from=2018-06-13&to=2018-07-02&pageNum=3&pageSize=25&service=autoru&sort=spending"
        ) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          xAuthorizationHeader ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[OfferProductsActivationsStatsListing] shouldBe stats
          verify(walletManager).getOfferProductsActivationsTotalStats(
            eq(dealer),
            eq(WalletListingParams(LocalDate.of(2018, 6, 13), Some(LocalDate.of(2018, 7, 2)), Some(3), Some(25))),
            eq(OfferSorting.Spending),
            eq(None),
            eq(None)
          )(?)
          verifyNoMoreInteractions(walletManager)
        }
      }
    }
  }

  "/dealer/call-record" should {

    val call = TeleponyClient.Call(
      id = "record-id",
      objectId = "dealer-12345",
      createTime = OffsetDateTime.now(),
      time = OffsetDateTime.now(),
      duration = 1,
      talkDuration = 1,
      callResult = TeleponyClient.CallResults.Success,
      recordId = Some("record-id"),
      redirectId = Some("abcd"),
      tag = None,
      source = "+79876543210",
      target = "+79876543210",
      proxy = "+79876543210",
      externalId = "ext:record_id"
    )

    "return free call record with empty `paid` param" in {
      forAll(DealerUserRefGen, SessionIdGen) { (dealer, sessionId) =>
        Resources.open("/telepony/call_record.wav") { (content) =>
          reset(teleponyCallsClient)
          reset(teleponyClient)

          val callRecord = CallRecord("record.wav", IOUtils.toByteArray(content))
          when(teleponyCallsClient.getCallRecord(?, ?, ?)(?)).thenReturnF(callRecord)

          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.CALL_TRACKING, AccessLevel.READ_ONLY).next
          }

          when(teleponyClient.getCall(?, ?)(?))
            .thenReturnF(call)
          when(dealerPonyClient.callInfo(?, ?, ?)(?))
            .thenReturnF(CallInfoResponse.newBuilder().setClientId(dealer.clientId).build())

          val recordId = "someCallRecord"

          Get(s"/1.0/dealer/call-record/$recordId") ~>
            xAuthorizationHeader ~>
            addHeader(Accept(`application/octet-stream`)) ~>
            addHeader("x-session-id", sessionId.toString) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            route ~> check {
            status shouldBe StatusCodes.OK
            header("Content-Disposition").map(_.value) shouldBe Some("attachment; filename=\"" + callRecord.name + "\"")
            verify(teleponyCallsClient).getCallRecord(eq("auto-dealers"), ?, eq(recordId))(?)
            verify(teleponyClient).getCall(eq("auto-dealers"), eq(recordId))(?)
          }
        }
      }
    }

    "return free call record with `paid` param = false" in {
      forAll(DealerUserRefGen, SessionIdGen) { (dealer, sessionId) =>
        Resources.open("/telepony/call_record.wav") { (content) =>
          reset(teleponyCallsClient)
          reset(teleponyClient)

          val callRecord = new CallRecord("record.wav", IOUtils.toByteArray(content))
          when(teleponyCallsClient.getCallRecord(?, ?, ?)(?)).thenReturnF(callRecord)

          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.CALL_TRACKING, AccessLevel.READ_ONLY).next
          }

          when(teleponyClient.getCall(?, ?)(?))
            .thenReturnF(call)
          when(dealerPonyClient.callInfo(?, ?, ?)(?))
            .thenReturnF(CallInfoResponse.newBuilder().setClientId(dealer.clientId).build())

          val recordId = "someCallRecord"

          Get(s"/1.0/dealer/call-record/$recordId?paid=false") ~>
            xAuthorizationHeader ~>
            addHeader(Accept(`application/octet-stream`)) ~>
            addHeader("x-session-id", sessionId.toString) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            route ~> check {
            status shouldBe StatusCodes.OK
            header("Content-Disposition").map(_.value) shouldBe Some("attachment; filename=\"" + callRecord.name + "\"")
            verify(teleponyCallsClient).getCallRecord(eq("auto-dealers"), ?, eq(recordId))(?)
            verify(teleponyClient).getCall(eq("auto-dealers"), eq(recordId))(?)
          }
        }
      }
    }

    "return free call record with `paid` param = true" in {
      forAll(DealerUserRefGen, SessionIdGen) { (dealer, sessionId) =>
        Resources.open("/telepony/call_record.wav") { (content) =>
          reset(teleponyCallsClient)
          reset(teleponyClient)

          val callRecord = new CallRecord("record.wav", IOUtils.toByteArray(content))
          when(teleponyCallsClient.getCallRecord(?, ?, ?)(?)).thenReturnF(callRecord)

          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.CALL_TRACKING, AccessLevel.READ_ONLY).next
          }

          when(teleponyClient.getCall(?, ?)(?))
            .thenReturnF(call)
          when(dealerPonyClient.callInfo(?, ?, ?)(?))
            .thenReturnF(CallInfoResponse.newBuilder().setClientId(dealer.clientId).build())

          val recordId = "someCallRecord"

          Get(s"/1.0/dealer/call-record/$recordId?paid=true") ~>
            xAuthorizationHeader ~>
            addHeader(Accept(`application/octet-stream`)) ~>
            addHeader("x-session-id", sessionId.toString) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            route ~> check {
            status shouldBe StatusCodes.OK
            header("Content-Disposition").map(_.value) shouldBe Some("attachment; filename=\"" + callRecord.name + "\"")
            verify(teleponyCallsClient).getCallRecord(eq("autoru_billing"), ?, eq(recordId))(?)
            verify(teleponyClient).getCall(eq("autoru_billing"), eq(recordId))(?)
          }
        }
      }
    }

    "return not found" in {
      forAll(DealerUserRefGen, SessionIdGen) { (dealer, sessionId) =>
        reset(teleponyCallsClient)
        reset(teleponyClient)

        when(teleponyCallsClient.getCallRecord(?, ?, ?)(?)).thenThrowF(new TeleponyRecordNotFound)

        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.CALL_TRACKING, AccessLevel.READ_ONLY).next
        }

        when(teleponyClient.getCall(?, ?)(?))
          .thenReturnF(call)
        when(dealerPonyClient.callInfo(?, ?, ?)(?))
          .thenReturnF(CallInfoResponse.newBuilder().setClientId(dealer.clientId).build())

        val recordId = "someCallRecord"

        Get(s"/1.0/dealer/call-record/$recordId") ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/octet-stream`)) ~>
          addHeader("x-session-id", sessionId.toString) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~> check {
          status shouldBe StatusCodes.NotFound
          verify(teleponyCallsClient).getCallRecord(eq("auto-dealers"), ?, eq(recordId))(?)
          verify(teleponyClient).getCall(eq("auto-dealers"), eq(recordId))(?)
        }
      }
    }

    "return call is not ready yet" in {
      forAll(DealerUserRefGen, SessionIdGen) { (dealer, sessionId) =>
        reset(teleponyCallsClient)
        reset(teleponyClient)

        when(teleponyCallsClient.getCallRecord(?, ?, ?)(?)).thenThrowF(new TeleponyRecordNotReady)

        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.CALL_TRACKING, AccessLevel.READ_ONLY).next
        }

        when(teleponyClient.getCall(?, ?)(?))
          .thenReturnF(call)
        when(dealerPonyClient.callInfo(?, ?, ?)(?))
          .thenReturnF(CallInfoResponse.newBuilder().setClientId(dealer.clientId).build())

        val recordId = "someCallRecord"

        Get(s"/1.0/dealer/call-record/$recordId") ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/octet-stream`)) ~>
          addHeader("x-session-id", sessionId.toString) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~> check {
          status shouldBe StatusCodes.NoContent
          verify(teleponyCallsClient).getCallRecord(eq("auto-dealers"), ?, eq(recordId))(?)
          verify(teleponyClient).getCall(eq("auto-dealers"), eq(recordId))(?)
        }
      }
    }

    "return forbidden" in {
      forAll(DealerUserRefGen, SessionIdGen) { (dealer, sessionId) =>
        reset(teleponyCallsClient)
        reset(teleponyClient)

        when(teleponyCallsClient.getCallRecord(?, ?, ?)(?)).thenThrowF(new TeleponyAccessException)

        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.CALL_TRACKING, AccessLevel.READ_ONLY).next
        }

        when(teleponyClient.getCall(?, ?)(?))
          .thenReturnF(call)
        when(dealerPonyClient.callInfo(?, ?, ?)(?))
          .thenReturnF(CallInfoResponse.newBuilder().setClientId(dealer.clientId).build())

        val recordId = "someCallRecord"

        Get(s"/1.0/dealer/call-record/$recordId") ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/octet-stream`)) ~>
          addHeader("x-session-id", sessionId.toString) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~> check {
          status shouldBe StatusCodes.Forbidden
          verify(teleponyCallsClient).getCallRecord(eq("auto-dealers"), ?, eq(recordId))(?)
          verify(teleponyClient).getCall(eq("auto-dealers"), eq(recordId))(?)
        }
      }
    }

    "return bad request" in {
      forAll(DealerUserRefGen, SessionIdGen) { (dealer, sessionId) =>
        reset(teleponyCallsClient)
        reset(teleponyClient)

        when(teleponyCallsClient.getCallRecord(?, ?, ?)(?)).thenThrowF(new TeleponyInvalidRequest)

        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.CALL_TRACKING, AccessLevel.READ_ONLY).next
        }

        when(teleponyClient.getCall(?, ?)(?))
          .thenReturnF(call)
        when(dealerPonyClient.callInfo(?, ?, ?)(?))
          .thenReturnF(CallInfoResponse.newBuilder().setClientId(dealer.clientId).build())

        val recordId = "someCallRecord"

        Get(s"/1.0/dealer/call-record/$recordId") ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/octet-stream`)) ~>
          addHeader("x-session-id", sessionId.toString) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~> check {
          status shouldBe StatusCodes.BadRequest
          verify(teleponyCallsClient).getCallRecord(eq("auto-dealers"), ?, eq(recordId))(?)
          verify(teleponyClient).getCall(eq("auto-dealers"), eq(recordId))(?)
        }
      }
    }
  }

  "/dealer/offers-daily-stats" should {
    "work" in {
      forAll(SessionResultGen, SessionIdGen, DealerUserRefGen, localDateInPast(), dealerDailyCountersResponse()) {
        (session, sessionId, dealer, from, response) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
          when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)

          when(countersManager.getDealerDailyStats(?, ?, ?, ?, ?)(?)).thenReturnF(response)

          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.DASHBOARD, AccessLevel.READ_ONLY).next
          }

          Get(s"/1.0/dealer/offers-daily-stats?from_date=$from&new=true") ~>
            xAuthorizationHeader ~>
            addHeader(Accept(`application/octet-stream`)) ~>
            addHeader("x-session-id", sessionId.toString) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[DealerDailyCountersResponse] shouldBe response

            verify(countersManager).getDealerDailyStats(
              eq(dealer),
              eq(None),
              eq(None),
              eq(from),
              eq(LocalDate.now())
            )(
              ?
            )
            verifyNoMoreInteractions(countersManager)
          }
      }
    }
  }

  "/dealer/users/{user}" should {
    "edit client user" in {
      forAll(SessionResultGen, DealerUserRefGen, PrivateUserRefGen) { (session, dealer, user) =>
        reset(passportClient, cabinetApiClient)

        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.USERS, AccessLevel.READ_WRITE).next
        }

        val groupId = 1L

        val accessGroup =
          DealerAccessGroupGen.next.toBuilder
            .setId(groupId)
            .build()

        when(cabinetApiClient.getClientAccessGroups(?)(?)).thenReturnF {
          GroupsList
            .newBuilder()
            .addGroups(accessGroup)
            .build()
        }

        when(passportClient.linkDealerUser(?, ?, ?)(?)).thenReturnF(())

        val expectedUser =
          User
            .newBuilder()
            .setId(user.uid.toString)
            .setActive(true)
            .setProfile {
              UserProfile
                .newBuilder()
                .setAutoru {
                  AutoruUserProfile
                    .newBuilder()
                    .setClientGroup(groupId.toString)
                }
            }
            .build()

        val usersList =
          UserIdsResult
            .newBuilder()
            .addUserIds(user.uid.toString)
            .addUsers(expectedUser)
            .build()

        when(passportClient.getDealerUsers(?)(?)).thenReturnF(usersList)

        val expectedResult =
          DealerUser
            .newBuilder()
            .setUser(expectedUser)
            .setAccess {
              AccessGrants
                .newBuilder()
                .setGroup(accessGroup.toBuilder().clearGrants())
                .addAllGrants(accessGroup.getGrantsList)
            }
            .build()

        Put(s"/1.0/dealer/user/${user.uid}?group=1") ~>
          xAuthorizationHeader ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[DealerUser] shouldBe expectedResult

          verify(cabinetApiClient, times(2)).getClientAccessGroups(eq(dealer))(?)
          verify(passportClient).linkDealerUser(eq(dealer), eq(user), eq("1"))(?)
          verify(passportClient).getDealerUsers(eq(dealer))(?)
        }
      }
    }

    "unlink client user" in {
      forAll(SessionResultGen, DealerUserRefGen, PrivateUserRefGen) { (session, dealer, user) =>
        reset(passportClient, cabinetApiClient)

        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.USERS, AccessLevel.READ_WRITE).next
        }

        when(passportClient.unlinkDealerUser(?, ?)(?)).thenReturnF(())

        Delete(s"/1.0/dealer/user/${user.uid}") ~>
          xAuthorizationHeader ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[SuccessResponse] shouldBe ManagerUtils.SuccessResponse

          verify(passportClient).unlinkDealerUser(eq(dealer), eq(user))(?)
        }
      }
    }
  }

  "/dealer/auction" should {

    "get current auction state" in {
      forAll(DealerUserRefGen, SessionResultGen, RegionGen, MarkModelsResponseGen) {
        (dealer, session, region, markModelsResponse) =>
          reset(auctionClient)
          when(passportClient.getSession(?)(?)).thenReturnF(session)

          when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
          when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_WRITE).next
          }
          when(dataService.tree).thenReturn(treeMock)
          when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
          when(vosClient.getDealerRegionId(?)(?)).thenReturnF(27L)
          when(vosClient.markModels(?, ?, ?)(?))
            .thenReturnF(markModelsResponse)
          when(auctionClient.currentStateByContextBatch(?)(?)).thenReturnF(AuctionStates.getDefaultInstance)
          when(auctionAutoStrategyClient.settingsForUser(?)(?))
            .thenReturnF(AutoStrategyListResponse.getDefaultInstance)
          when(featureManager.enableAuctionAutoStrategy).thenReturn(new Feature[Boolean] {
            override def name: String = "enable_auction_auto_strategy"
            override def value: Boolean = false
          })
          Get(s"/1.0/dealer/auction/current-state") ~>
            xAuthorizationHeader ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            route ~> check {
            status shouldBe StatusCodes.OK
          }
      }
    }

    "make bid without previous bid in context" in {
      val bidRequest = HttpEntity(
        ContentTypes.`application/json`,
        """
          |{
          |  "context": {
          |    "mark_code": "BMW",
          |    "model_code": "X5"
          |  },
          |
          |  "bid": 1000
          |}
          |""".stripMargin
      )

      when(auctionClient.placeBid(?)(?)).thenReturn(Future.unit)
      forAll(DealerUserRefGen, SessionResultGen, RegionGen, MarkModelsResponseGen) {
        (dealer, session, region, markModelsResponse) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_WRITE).next
          }
          when(dataService.tree).thenReturn(treeMock)
          when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
          when(vosClient.getDealerRegionId(?)(?)).thenReturnF(27L)
          when(vosClient.markModels(?, ?, ?)(?))
            .thenReturnF(markModelsResponse)
          when(dataService.tree).thenReturn(treeMock)
          when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
          when(vosClient.getDealerRegionId(?)(?)).thenReturnF(27L)
          when(vosClient.markModels(?, ?, ?)(?))
            .thenReturnF(markModelsResponse)
          when(catalogClient.filter(?, ?)(?)).thenReturnF {
            RawCatalog
              .newBuilder()
              .putMark(
                "BMW",
                MarkCard.newBuilder
                  .putModel("X5", ModelCard.getDefaultInstance)
                  .build()
              )
              .build
          }
          when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
          Post(s"/1.0/dealer/auction/place-bid", bidRequest) ~>
            xAuthorizationHeader ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            route ~> check {
            status shouldBe StatusCodes.OK
          }
      }
    }

    "leave auction with previous context" in {
      val stopRequest = HttpEntity(
        ContentTypes.`application/json`,
        """
          |{
          |  "context": {
          |    "mark_code": "BMW",
          |    "model_code": "X5"
          |  },
          |  "previous_bid": 1100
          |}
          |""".stripMargin
      )
      when(auctionClient.stopAuction(?)(?)).thenReturn(Future.unit)
      forAll(DealerUserRefGen, SessionResultGen, RegionGen, MarkModelsResponseGen) {
        (dealer, session, region, markModelsResponse) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_WRITE).next
          }
          when(dataService.tree).thenReturn(treeMock)
          when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
          when(vosClient.getDealerRegionId(?)(?)).thenReturnF(27L)
          when(vosClient.markModels(?, ?, ?)(?))
            .thenReturnF(markModelsResponse)
          when(catalogClient.filter(?, ?)(?)).thenReturnF {
            RawCatalog
              .newBuilder()
              .putMark(
                "BMW",
                MarkCard.newBuilder
                  .putModel("X5", ModelCard.getDefaultInstance)
                  .build()
              )
              .build
          }
          when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
          Post(s"/1.0/dealer/auction/leave", stopRequest) ~>
            xAuthorizationHeader ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            route ~> check {
            status shouldBe StatusCodes.OK
          }
      }
    }

    "make auto strategy " in {
      val autoStrategyChangeRequst = HttpEntity(
        ContentTypes.`application/json`,
        """{
          |   "context": {
          |     "mark_code": "HONDA",
          |     "model_code": "CIVIC"
          |   },
          |   "auto_strategy": {
          |     "max_bid": 1999,
          |     "max_position_for_price": {}
          |   }
          | }""".stripMargin
      )
      when(auctionClient.stopAuction(?)(?)).thenReturn(Future.unit)
      forAll(DealerUserRefGen, SessionResultGen, RegionGen, MarkModelsResponseGen) {
        (dealer, session, region, markModelsResponse) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_WRITE).next
          }
          when(dataService.tree).thenReturn(treeMock)
          when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
          when(vosClient.getDealerRegionId(?)(?)).thenReturnF(27L)
          when(vosClient.markModels(?, ?, ?)(?))
            .thenReturnF(markModelsResponse)
          when(catalogClient.filter(?, ?)(?)).thenReturnF {
            RawCatalog
              .newBuilder()
              .putMark(
                "HONDA",
                MarkCard.newBuilder
                  .putModel("CIVIC", ModelCard.getDefaultInstance)
                  .build()
              )
              .build
          }
          when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
          when(auctionAutoStrategyClient.create(?)(?)).thenReturn(Future.unit)
          Post(s"/1.0/dealer/auction/auto/strategy/change", autoStrategyChangeRequst) ~>
            xAuthorizationHeader ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            route ~> check {
            status shouldBe StatusCodes.OK
          }
      }
    }

    "delete auto strategy " in {
      val autoStrategyChangeRequst = HttpEntity(
        ContentTypes.`application/json`,
        """{
          |   "context": {
          |     "mark_code": "HONDA",
          |     "model_code": "CIVIC"
          |   }
          | }""".stripMargin
      )
      when(auctionClient.stopAuction(?)(?)).thenReturn(Future.unit)
      forAll(DealerUserRefGen, SessionResultGen, RegionGen, MarkModelsResponseGen) {
        (dealer, session, region, markModelsResponse) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_WRITE).next
          }
          when(dataService.tree).thenReturn(treeMock)
          when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
          when(vosClient.getDealerRegionId(?)(?)).thenReturnF(27L)
          when(vosClient.markModels(?, ?, ?)(?))
            .thenReturnF(markModelsResponse)
          when(catalogClient.filter(?, ?)(?)).thenReturnF {
            RawCatalog
              .newBuilder()
              .putMark(
                "HONDA",
                MarkCard.newBuilder
                  .putModel("CIVIC", ModelCard.getDefaultInstance)
                  .build()
              )
              .build
          }
          when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
          when(auctionAutoStrategyClient.remove(?)(?)).thenReturn(Future.unit)
          Post(s"/1.0/dealer/auction/auto/strategy/delete", autoStrategyChangeRequst) ~>
            xAuthorizationHeader ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            addHeader("x-dealer-id", dealer.clientId.toString) ~>
            route ~> check {
            status shouldBe StatusCodes.OK
          }
      }
    }
  }

  "GET /dealer/wallet/avito/daily-stats" should {
    def setupMocks(session: SessionResult, avitoWalletResponse: AvitoWalletDailyOperationListingResponse) = {
      reset(multipostingClient)

      when(passportClient.getSession(?)(?))
        .thenReturnF(session)
      when(cabinetApiClient.checkAccess(?, ?)(?))
        .thenReturnF(checkAccessClientView)
      when(passportClient.getUserEssentials(?, ?)(?))
        .thenReturnF(DealerUserEssentialsGen.next)
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
      }

      when(multipostingClient.getAvitoWalletDailyStats(?, ?)(?))
        .thenReturnF(avitoWalletResponse)
    }

    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "call multiposting" in {
      forAll(DealerUserRefGen, SessionResultGen, GetAvitoWalletDailyOperationListResponseGen) {
        (dealer, session, avitoWalletResponse) =>

          val response = AvitoWalletDailyOperationListingResponse
            .newBuilder()
            .addAllOperations(avitoWalletResponse.getDaysList)
            .setStatus(ResponseStatus.SUCCESS)
            .build()

          setupMocks(session, response)

          Get(
            s"/1.0/dealer/wallet/avito/daily-stats" +
              s"?from_date=2021-05-13" +
              s"&to_date=2023-06-12"
          ) ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[AvitoWalletDailyOperationListingResponse].getOperationsList shouldBe avitoWalletResponse.getDaysList

            val from = LocalDate.of(2021, 5, 13)
            val to = LocalDate.of(2023, 6, 12)

            val request = AvitoWalletOperationTotalStatsFilter(from, Some(to))

            verify(multipostingClient).getAvitoWalletDailyStats(dealer = eq(dealer), eq(request))(?)
            verifyNoMoreInteractions(multipostingClient)
          }
      }
    }

    "should call multiposting with default parameters" in {
      forAll(DealerUserRefGen, SessionResultGen, GetAvitoWalletDailyOperationListResponseGen) {
        (dealer, session, avitoWalletResponse) =>

          val response = AvitoWalletDailyOperationListingResponse
            .newBuilder()
            .addAllOperations(avitoWalletResponse.getDaysList)
            .setStatus(ResponseStatus.SUCCESS)
            .build()

          setupMocks(session, response)

          Get(
            s"/1.0/dealer/wallet/avito/daily-stats" +
              s"?from_date=2021-05-13"
          ) ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[AvitoWalletDailyOperationListingResponse].getOperationsList shouldBe avitoWalletResponse.getDaysList

            val from = LocalDate.of(2021, 5, 13)

            val request = AvitoWalletOperationTotalStatsFilter(from, None)

            verify(multipostingClient).getAvitoWalletDailyStats(dealer = eq(dealer), eq(request))(?)
            verifyNoMoreInteractions(multipostingClient)
          }
      }
    }
  }

  "GET /dealer/wallet/avito" should {
    def setupMocks(session: SessionResult, avitoWalletBalanceResponse: GetAvitoWalletBalanceResponse) = {
      reset(multipostingClient)

      when(passportClient.getSession(?)(?))
        .thenReturnF(session)
      when(cabinetApiClient.checkAccess(?, ?)(?))
        .thenReturnF(checkAccessClientView)
      when(passportClient.getUserEssentials(?, ?)(?))
        .thenReturnF(DealerUserEssentialsGen.next)
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
      }

      when(multipostingClient.getAvitoWalletBalance(?)(?))
        .thenReturnF(avitoWalletBalanceResponse)
    }

    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "call AvitoWalletBalance" in {
      forAll(DealerUserRefGen, SessionResultGen, GetAvitoWalletBalanceResponseGen) {
        (dealer, session, avitoWalletResponse) =>

          setupMocks(session, avitoWalletResponse)

          Get("/1.0/dealer/wallet/avito") ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[GetAvitoWalletBalanceResponse].getBalance shouldBe avitoWalletResponse.getBalance
          }
      }
    }
  }

  "GET /dealer/wallet/avito/tariff" should {
    def setupMocks(session: SessionResult, response: GetAvitoTariffResponse) = {
      reset(multipostingClient)

      when(passportClient.getSession(?)(?))
        .thenReturnF(session)
      when(cabinetApiClient.checkAccess(?, ?)(?))
        .thenReturnF(checkAccessClientView)
      when(passportClient.getUserEssentials(?, ?)(?))
        .thenReturnF(DealerUserEssentialsGen.next)
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.WALLET, AccessLevel.READ_ONLY).next
      }

      when(multipostingClient.getAvitoTariffInfo(?)(?)).thenReturnF(response)
    }

    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "call AvitoTariff" in {
      forAll(DealerUserRefGen, SessionResultGen, GetAvitoTariffResponseGen) { (dealer, session, response) =>

        setupMocks(session, response)

        Get("/1.0/dealer/wallet/avito/tariff") ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[GetAvitoTariffResponse] shouldBe response
        }
      }
    }
  }

  "POST /dealer/phones/whitelist/add" should {
    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "call dealer-pony white list add" in {
      forAll(DealerUserRefGen, SessionResultGen, SimplePhonesListGen) { (dealer, session, addPhoneNumbersRequestGen) =>
        reset(dealerPonyClient)
        when(passportClient.getSession(?)(?))
          .thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?))
          .thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?))
          .thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.SETTINGS, AccessLevel.READ_WRITE).next
        }

        val result = getPhonesResultList(addPhoneNumbersRequestGen).next

        when(dealerPonyClient.addPhoneNumbers(?)(?))
          .thenReturnF(result)

        Post(
          s"/1.0/dealer/phones/whitelist/add",
          addPhoneNumbersRequestGen
        ) ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[PhonesResultList] shouldBe result

          verify(dealerPonyClient)
            .addPhoneNumbers(request =
              eq(
                AddPhoneNumbersRequest.newBuilder
                  .setClientId(dealer.clientId)
                  .setPhones(addPhoneNumbersRequestGen)
                  .build()
              )
            )(?)
          verifyNoMoreInteractions(dealerPonyClient)
        }
      }
    }

    "call dealer-pony white list add and api error" in {
      forAll(DealerUserRefGen, SessionResultGen, SimplePhonesListGen) { (dealer, session, addPhoneNumbersRequestGen) =>
        reset(dealerPonyClient)
        when(passportClient.getSession(?)(?))
          .thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?))
          .thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?))
          .thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.SETTINGS, AccessLevel.READ_WRITE).next
        }

        val result = getPhonesResultList(addPhoneNumbersRequestGen).next

        when(dealerPonyClient.addPhoneNumbers(?)(?))
          .thenReturnF(result)

        Post(
          s"/1.0/dealer/phones/whitelist/add",
          addPhoneNumbersRequestGen
        ) ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[PhonesResultList] shouldBe result

          verify(dealerPonyClient)
            .addPhoneNumbers(request =
              eq(
                AddPhoneNumbersRequest.newBuilder
                  .setClientId(dealer.clientId)
                  .setPhones(addPhoneNumbersRequestGen)
                  .build()
              )
            )(?)
          verifyNoMoreInteractions(dealerPonyClient)
        }
      }
    }
  }

  "POST /dealer/phones/whitelist/delete" should {
    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "call dealer-pony white list delete" in {
      forAll(DealerUserRefGen, SessionResultGen, SimplePhonesListGen) {
        (dealer, session, deletePhoneNumbersRequestGen) =>
          reset(dealerPonyClient)
          when(passportClient.getSession(?)(?))
            .thenReturnF(session)
          when(cabinetApiClient.checkAccess(?, ?)(?))
            .thenReturnF(checkAccessClientView)
          when(passportClient.getUserEssentials(?, ?)(?))
            .thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.SETTINGS, AccessLevel.READ_WRITE).next
          }

          val result = getPhonesResultList(deletePhoneNumbersRequestGen).next

          when(dealerPonyClient.deletePhoneNumbers(?)(?)).thenReturnF(result)

          Post(
            s"/1.0/dealer/phones/whitelist/delete",
            deletePhoneNumbersRequestGen
          ) ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[PhonesResultList] shouldBe result

            verify(dealerPonyClient).deletePhoneNumbers(request =
              eq(
                DeletePhoneNumbersRequest.newBuilder
                  .setClientId(dealer.clientId)
                  .setPhones(deletePhoneNumbersRequestGen)
                  .build()
              )
            )(?)
            verifyNoMoreInteractions(dealerPonyClient)
          }
      }
    }
  }

  "POST /dealer/phones/whitelist/get" should {
    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "call dealer-pony white list get" in {
      forAll(DealerUserRefGen, SessionResultGen, SimplePhonesListGen) { (dealer, session, simplePhonesListGen) =>
        reset(dealerPonyClient)
        when(passportClient.getSession(?)(?))
          .thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?))
          .thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?))
          .thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.SETTINGS, AccessLevel.READ_ONLY).next
        }
        when(dealerPonyClient.listPhoneNumbers(?)(?))
          .thenReturnF(simplePhonesListGen)

        Post(
          s"/1.0/dealer/phones/whitelist/get"
        ) ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[SimplePhonesList] shouldBe simplePhonesListGen

          verify(dealerPonyClient).listPhoneNumbers(dealerId = eq(dealer.clientId))(?)
          verifyNoMoreInteractions(dealerPonyClient)
        }
      }
    }
  }

  "POST /dealer/phones/whitelist/available" should {
    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "call dealer-pony white list available" in {

      forAll(DealerUserRefGen, SessionResultGen, BooleanGen.map(BoolValue.of)) { (dealer, session, booleanGen) =>
        reset(dealerPonyClient)
        when(passportClient.getSession(?)(?))
          .thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?))
          .thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?))
          .thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.SETTINGS, AccessLevel.READ_ONLY).next
        }
        when(dealerPonyClient.whiteListAvailable(?)(?))
          .thenReturnF(booleanGen)

        Post(
          s"/1.0/dealer/phones/whitelist/available"
        ) ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[BoolValue] shouldBe booleanGen

          verify(dealerPonyClient).whiteListAvailable(dealerId = eq(dealer.clientId))(?)
          verifyNoMoreInteractions(dealerPonyClient)
        }
      }
    }
  }

  "POST /dealer/phones/whitelist/entries-left" should {
    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "call dealer-pony entries left" in {
      forAll(DealerUserRefGen, SessionResultGen, Gen.posNum[Int].map(Int32Value.of)) {
        (dealer, session, dealerPonyEntriesLeftResponse) =>
          reset(dealerPonyClient)
          when(passportClient.getSession(?)(?))
            .thenReturnF(session)
          when(cabinetApiClient.checkAccess(?, ?)(?))
            .thenReturnF(checkAccessClientView)
          when(passportClient.getUserEssentials(?, ?)(?))
            .thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.SETTINGS, AccessLevel.READ_ONLY).next
          }
          when(dealerPonyClient.phoneEntriesLeft(?)(?))
            .thenReturnF(dealerPonyEntriesLeftResponse)
          Post(
            s"/1.0/dealer/phones/whitelist/entries-left"
          ) ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[Int32Value] shouldBe dealerPonyEntriesLeftResponse

            verify(dealerPonyClient).phoneEntriesLeft(dealerId = eq(dealer.clientId))(?)
            verifyNoMoreInteractions(dealerPonyClient)
          }
      }
    }
  }

  "GET /dealer/warehouse/widget" should {
    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "return dealer warehouse indicators" in {
      forAll(DealerWarehouseIndicatorsResponseGen, DealerUserRefGen, SessionResultGen) {
        case (warehouseIndicator, dealer, session) =>
          reset(dealerStatsManager)
          when(passportClient.getSession(?)(?))
            .thenReturnF(session)
          when(cabinetApiClient.checkAccess(?, ?)(?))
            .thenReturnF(checkAccessClientView)
          when(passportClient.getUserEssentials(?, ?)(?))
            .thenReturnF(DealerUserEssentialsGen.next)
          when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
            dealerAccessGroupWithGrantGen(ResourceAlias.DASHBOARD, AccessLevel.READ_ONLY).next
          }

          when(dealerStatsManager.getWarehouseIndicator(?)(?, ?))
            .thenReturnF(warehouseIndicator)

          Get("/1.0/dealer/warehouse/widget") ~> defaultHttpHeaders(session, dealer) ~> route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[DealerWarehouseIndicatorsResponse] shouldBe warehouseIndicator

            verify(dealerStatsManager).getWarehouseIndicator(eq(dealer.clientId))(?, ?)
            verifyNoMoreInteractions(dealerStatsManager)
          }
      }
    }
  }

  "POST /dealer/agency" should {
    def defaultHttpHeaders(session: SessionResult, dealer: AutoruDealer) =
      xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", dealer.clientId.toString)

    "update agency successfully" in {

      val dealer = DealerUserRefGen.next
      val session = SessionResultGen.next
      val agencyId = 123L

      val clientView = ClientView(
        properties = ClientPropertiesView(
          status = "new",
          regionId = 123L
        )
      )

      when(passportClient.getSession(?)(?))
        .thenReturnF(session)
      when(cabinetApiClient.checkAccess(?, ?)(?))
        .thenReturnF(checkAccessClientView)
      when(passportClient.getUserEssentials(?, ?)(?))
        .thenReturnF(DealerUserEssentialsGen.next)
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.SALON_REQUISITES, AccessLevel.READ_WRITE).next
      }

      when(cabinetApiClient.getClient(?)(?)).thenReturnF(clientView)
      when(cabinetApiClient.updateAgency(?, ?)(?)).thenReturnF(ManagerUtils.SuccessResponse)

      Put(s"/1.0/dealer/agency/$agencyId") ~>
        defaultHttpHeaders(session, dealer) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[SuccessResponse] shouldBe ManagerUtils.SuccessResponse

          verify(cabinetApiClient).getClient(clientId = eq(dealer.clientId))(?)
          verify(cabinetApiClient).updateAgency(clientId = eq(dealer.clientId), eq(agencyId))(?)
        }
    }
  }
}
