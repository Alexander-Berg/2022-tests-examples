package ru.auto.api.managers.wallet

import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.ResponseStatus.SUCCESS
import ru.auto.api.ResponseModel.{Filters, OfferListingResponse, Pagination}
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.balance.BalanceManager
import ru.auto.api.managers.dealer.DealerStatsManager
import ru.auto.api.managers.decay.DecayOptions
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.model.AutoruProduct.Placement
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils.RichOfferOrBuilder
import ru.auto.api.model.billing.vsbilling.{OrderId, OrderTransactionsResponse, OrdersResponse, Page}
import ru.auto.api.model.billing.{BalanceClient, BalanceId}
import ru.auto.api.model.{dealer => _, _}
import ru.auto.api.services.billing.VsBillingClient
import ru.auto.api.services.billing.VsBillingClient.OrderTransactionsParams
import ru.auto.api.services.billing.VsBillingTestUtils.{from => _, params => _, to => _, _}
import ru.auto.api.services.statistics.MetricsTestData.{from => _, params => _, to => _, _}
import ru.auto.api.util.TimeUtils.TimeProvider
import ru.auto.dealer_stats.proto.Rpc.{GetOfferProductActivationsDailyStatsResponse, TotalStat}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class WalletManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with TestRequest
  with BeforeAndAfter {

  private val vsBillingClient = mock[VsBillingClient]
  private val dealerStatsManager = mock[DealerStatsManager]
  private val balanceManager = mock[BalanceManager]
  private val offerLoader = mock[EnrichedOfferLoader]
  private val timeProvider = mock[TimeProvider]

  private val manager =
    new WalletManager(
      vsBillingClient,
      dealerStatsManager,
      balanceManager,
      offerLoader,
      timeProvider
    )

  before {
    reset(vsBillingClient, balanceManager)
  }

  after {
    verifyNoMoreInteractions(vsBillingClient, balanceManager)
  }

  "WalletManager.getRecharges()" should {

    import ru.auto.api.services.billing.VsBillingTestUtils.{from, to}

    "return recharge for dealer" in {
      forAll(balanceIdGen) { dealerBalanceId =>
        reset(vsBillingClient, balanceManager)
        val total = 10
        val page = Page(size = 1, number = 10)
        val orderResponse = OrdersResponse(total, page, dealerOrders)
        val orderTransactionResponse = OrderTransactionsResponse(total, page, dealerOrderTransactions)
        val params = WalletListingParams(from, Some(to), None, None)
        val expectedParams =
          OrderTransactionsParams(from, Some(to.plusDays(1)), None, None, Some("Incoming"), Some(true))
        when(balanceManager.getBalanceClient()(?)).thenReturnF(BalanceClient(Some(1L), dealerBalanceId, None))
        when(vsBillingClient.getOrders(BalanceId(?), ?)(?)).thenReturnF(orderResponse)
        when(vsBillingClient.getOrderTransactions(BalanceId(?), ?, OrderId(?), ?)(?))
          .thenReturnF(orderTransactionResponse)
        val res = manager.getRecharges(params).futureValue
        res.getStatus shouldBe SUCCESS
        val List(recharge) = res.getRechargesList.asScala.toList: @unchecked
        val timestamp = recharge.getTimestamp
        timestamp.getSeconds shouldBe dealerTransactionTimestampSeconds
        timestamp.getNanos shouldBe dealerTransactionTimestampNanos
        recharge.getAmount shouldBe dealerTransactionAmountRub
        verify(balanceManager).getBalanceClient()(?)
        verify(vsBillingClient).getOrders(BalanceId(eq(dealerBalanceId.value)), eq(None))(?)
        verify(vsBillingClient).getOrderTransactions(
          BalanceId(eq(dealerBalanceId.value)),
          eq(None),
          OrderId(eq(dealerOrderId.value)),
          eq(expectedParams)
        )(?)
        verifyNoMoreInteractions(vsBillingClient, balanceManager)
      }
    }

    "return recharge for agency" in {
      forAll(balanceIdGen, balanceIdGen) { (dealerBalanceId, agencyBalanceId) =>
        reset(vsBillingClient, balanceManager)
        val total = 10
        val page = Page(size = 1, number = 10)
        val orderResponse = OrdersResponse(total, page, agencyOrders)
        val orderTransactionResponse = OrderTransactionsResponse(total, page, agencyOrderTransactions)
        val params = WalletListingParams(from, Some(to), None, None)
        val expectedParams =
          OrderTransactionsParams(from, Some(to.plusDays(1)), None, None, Some("Incoming"), Some(true))
        when(balanceManager.getBalanceClient()(?))
          .thenReturnF(BalanceClient(Some(1L), dealerBalanceId, Some(agencyBalanceId)))
        when(vsBillingClient.getOrders(BalanceId(?), ?)(?)).thenReturnF(orderResponse)
        when(vsBillingClient.getOrderTransactions(BalanceId(?), ?, OrderId(?), ?)(?))
          .thenReturnF(orderTransactionResponse)
        val res = manager.getRecharges(params).futureValue
        res.getStatus shouldBe SUCCESS
        val List(recharge) = res.getRechargesList.asScala.toList: @unchecked
        val timestamp = recharge.getTimestamp
        timestamp.getSeconds shouldBe agencyTransactionTimestampSeconds
        timestamp.getNanos shouldBe agencyTransactionTimestampNanos
        recharge.getAmount shouldBe agencyTransactionAmountRub
        verify(balanceManager).getBalanceClient()(?)
        verify(vsBillingClient)
          .getOrders(BalanceId(eq(dealerBalanceId.value)), eq(Some(BalanceId(agencyBalanceId.value))))(?)
        verify(vsBillingClient).getOrderTransactions(
          BalanceId(eq(dealerBalanceId.value)),
          eq(Some(BalanceId(agencyBalanceId.value))),
          OrderId(eq(agencyOrderId.value)),
          eq(expectedParams)
        )(?)
        verifyNoMoreInteractions(vsBillingClient, balanceManager)
      }
    }

    "return full recharges for dealer" in {
      forAll(balanceIdGen) { dealerBalanceId =>
        reset(vsBillingClient, balanceManager)
        val total = 100
        val page = Page(size = 3, number = 15)
        val orderResponse = OrdersResponse(total, Page(size = 1, number = 0), fullOrders)
        val orderTransactionsResponse = OrderTransactionsResponse(total, page, fullOrderTransactions)
        val params = WalletListingParams(from, Some(to), Some(16), Some(3))
        val expectedParams =
          OrderTransactionsParams(from, Some(to.plusDays(1)), Some(15), Some(3), Some("Incoming"), Some(true))
        when(balanceManager.getBalanceClient()(?)).thenReturnF(BalanceClient(Some(1L), dealerBalanceId, None))
        when(vsBillingClient.getOrders(BalanceId(?), ?)(?)).thenReturnF(orderResponse)
        when(vsBillingClient.getOrderTransactions(BalanceId(?), ?, OrderId(?), ?)(?))
          .thenReturnF(orderTransactionsResponse)
        val res = manager.getRecharges(params).futureValue
        res.getStatus shouldBe SUCCESS
        val paging = res.getPaging
        paging.getTotal shouldBe total
        paging.getPage.getNum shouldBe 16
        paging.getPage.getSize shouldBe 3
        paging.getPageCount shouldBe 34
        val List(recharge0, recharge1, recharge2) = res.getRechargesList.asScala.toList: @unchecked
        val timestamp0 = recharge0.getTimestamp
        timestamp0.getSeconds shouldBe fullTransaction0TimestampSeconds
        timestamp0.getNanos shouldBe 0
        recharge0.getAmount shouldBe fullTransaction0AmountRub
        val timestamp1 = recharge1.getTimestamp
        timestamp1.getSeconds shouldBe fullTransaction1TimestampSeconds
        timestamp1.getNanos shouldBe 0
        recharge1.getAmount shouldBe fullTransaction1AmountRub
        val timestamp2 = recharge2.getTimestamp
        timestamp2.getSeconds shouldBe fullTransaction2TimestampSeconds
        timestamp2.getNanos shouldBe 0
        recharge2.getAmount shouldBe fullTransaction2AmountRub
        verify(balanceManager).getBalanceClient()(?)
        verify(vsBillingClient).getOrders(BalanceId(eq(dealerBalanceId.value)), eq(None))(?)
        verify(vsBillingClient).getOrderTransactions(
          BalanceId(eq(dealerBalanceId.value)),
          eq(None),
          OrderId(eq(fullOrderId.value)),
          eq(expectedParams)
        )(?)
        verifyNoMoreInteractions(vsBillingClient, balanceManager)
      }
    }

    "not return recharges if no orders" in {
      forAll(balanceIdGen) { dealerBalanceId =>
        reset(vsBillingClient, balanceManager)
        val total = 10
        val page = Page(size = 1, number = 10)
        val orderResponse = OrdersResponse(total, page, Nil)
        val params = WalletListingParams(from, Some(to), None, None)
        when(balanceManager.getBalanceClient()(?)).thenReturnF(BalanceClient(Some(1L), dealerBalanceId, None))
        when(vsBillingClient.getOrders(BalanceId(?), ?)(?)).thenReturnF(orderResponse)
        val res = manager.getRecharges(params).failed.futureValue
        res shouldBe an[Exception]
        verify(balanceManager).getBalanceClient()(?)
        verify(vsBillingClient).getOrders(BalanceId(eq(dealerBalanceId.value)), eq(None))(?)
        verifyNoMoreInteractions(vsBillingClient, balanceManager)
      }
    }

    "not return recharges if more than one order" in {
      forAll(balanceIdGen) { dealerBalanceId =>
        reset(vsBillingClient, balanceManager)
        val total = 10
        val page = Page(size = 1, number = 10)
        val orderResponse = OrdersResponse(total, page, dealerOrders ++ agencyOrders)
        val params = WalletListingParams(from, Some(to), None, None)
        when(balanceManager.getBalanceClient()(?)).thenReturnF(BalanceClient(Some(1L), dealerBalanceId, None))
        when(vsBillingClient.getOrders(BalanceId(?), ?)(?)).thenReturnF(orderResponse)
        val res = manager.getRecharges(params).failed.futureValue
        res shouldBe an[Exception]
        verify(balanceManager).getBalanceClient()(?)
        verify(vsBillingClient).getOrders(BalanceId(eq(dealerBalanceId.value)), eq(None))(?)
        verifyNoMoreInteractions(vsBillingClient, balanceManager)
      }
    }
  }

  "WalletManager.getProductActivationsDailyStats()" should {

    import ru.auto.api.services.statistics.MetricsTestData.{params, to}

    "get stats without products filter" in {
      when(dealerStatsManager.getProductActivationsDailyStats(?, ?, ?, ?)(?))
        .thenReturnF(productActivationsDailyStats)
      val res = manager
        .getProductActivationsDailyStats(dealer, params.copy(pageNum = Some(1)), None)
        .futureValue
      checkStats(res.getActivationStatsList.asScala.toVector)
      checkPaging(res.getPaging)
    }

    "get stats with products filter" in {
      when(dealerStatsManager.getProductActivationsDailyStats(?, ?, ?, ?)(?))
        .thenReturnF(productActivationsDailyStats)
      val res = manager
        .getProductActivationsDailyStats(
          dealer,
          params.copy(pageNum = Some(1), pageSize = Some(2)),
          Some(List(MetricProduct.Placement))
        )
        .futureValue

      res.getActivationStatsList.asScala.size shouldBe 2

    }

    "get single stat" in {
      when(timeProvider.currentLocalDate()).thenReturn(to)
      when(dealerStatsManager.getProductActivationsDailyStats(?, ?, ?, ?)(?))
        .thenReturnF(
          productActivationsDailyStats.toBuilder
            .clearActivationStats()
            .addAllActivationStats(Seq(metric1).asJava)
            .setTotal(
              TotalStat.newBuilder().setCount(metric1.getCount).setSpentKopecks(metric1.getSpentKopecks).build()
            )
            .build()
        )
      val res = manager.getProductActivationsDailyStats(dealer, altParams, None).futureValue
      val stats = res.getActivationStatsList.asScala
      stats should have size 1
      val stat = stats.head
      stat.getProduct shouldBe "placement"
      stat.getDate shouldBe "2018-06-14"
      stat.getCount shouldBe 553
      stat.getSum shouldBe 57910
      val paging = res.getPaging
      paging.getTotal shouldBe 1
      paging.getPageCount shouldBe 1
      val page = paging.getPage
      page.getNum shouldBe 1
      page.getSize shouldBe pageSize
    }

  }

  "WalletManager.getGroupedProductActivationsDailyStats()" should {

    "get stats for offers" in {
      forAll(truckOfferGen("15281737-85d7dfdc"), truckOfferGen("15281739-4bb645e9")) { (firstOffer, secondOffer) =>
        val listing = OfferListingResponse
          .newBuilder()
          .addOffers(firstOffer)
          .addOffers(secondOffer)
          .setPagination(Pagination.newBuilder().setPage(1).setPageSize(2).setTotalOffersCount(2).setTotalPageCount(1))
          .build()

        when(
          dealerStatsManager.getOfferProductActivationsDailyStats(
            ?,
            ?,
            ?,
            ?,
            ?
          )(?)
        ).thenReturnF(placementActivations)

        when(offerLoader.getListing(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturnF(listing)
        val res = manager
          .getGroupedProductActivationsDailyStats(
            dealer,
            Placement,
            detailedActivationsDate,
            pageNum = 1,
            pageSize = 2
          )
          .futureValue
        val paging = res.getPaging
        paging.getPageCount shouldBe 1
        val page = paging.getPage
        page.getNum shouldBe 1
        page.getSize shouldBe 2
        val List(firstOfferStat, secondOfferStat) = res.getOfferProductActivationsStatsList.asScala.toList: @unchecked
        firstOfferStat.getOffer shouldBe firstOffer
        val firstStat = firstOfferStat.getStatsList.asScala.toList.head
        firstStat.getDate shouldBe "2018-06-17"
        firstStat.getProduct shouldBe "placement"
        firstStat.getSum shouldBe 10
        firstStat.getCount shouldBe 1
        val List(secondStat) = secondOfferStat.getStatsList.asScala.toList: @unchecked
        secondStat.getDate shouldBe "2018-06-17"
        secondStat.getProduct shouldBe "placement"
        secondStat.getSum shouldBe 40
        secondStat.getCount shouldBe 1
        verify(offerLoader).getListing(
          eq(CategorySelector.All),
          eq(dealer),
          argThat[Paging] { paging =>
            val bool = paging.page == 1 && paging.pageSize == 2
            bool
          },
          argThat[Filters] { filter =>
            val iRefs = filter.getOfferIRefList.asScala
            iRefs.size == 2 && iRefs.map(_.longValue()).toSet == Set(15281737L, 15281739L)
          },
          eq(NoSorting),
          includeRemoved = eq(true),
          // mockito passes nulls as default arguments
          eq(EnrichOptions(techParams = true)),
          eq(DecayOptions.ForOwner)
        )(?)
        // don't check for other interactions with offerLoader,
        // because mockito sees one invocation with default arguments as several invocations
        reset(offerLoader)
      }
    }

    "get empty stats if pageNum is too high" in {
      when(
        dealerStatsManager.getOfferProductActivationsDailyStats(
          ?,
          ?,
          ?,
          ?,
          ?
        )(?)
      ).thenReturnF(GetOfferProductActivationsDailyStatsResponse.getDefaultInstance)
      val res = manager
        .getGroupedProductActivationsDailyStats(
          dealer,
          Placement,
          detailedActivationsDate,
          pageNum = 2,
          pageSize = 2
        )
        .futureValue
      val paging = res.getPaging
      paging.getPageCount shouldBe 0
      val page = paging.getPage
      page.getNum shouldBe 2
      page.getSize shouldBe 2
      res.getOfferProductActivationsStatsList shouldBe empty
    }

    "throw if page size is too high" in {
      val res = intercept[IllegalArgumentException] {
        throw manager
          .getGroupedProductActivationsDailyStats(
            dealer,
            Placement,
            detailedActivationsDate,
            pageNum = 0,
            pageSize = 81
          )
          .failed
          .futureValue
      }
      res.getMessage shouldBe "pageSize shouldn't be more than 80"
    }
  }

  "WalletManager.getProductActivationsStats()" should {

    import ru.auto.api.services.statistics.MetricsTestData.{from, to}

    "get stats" in {
      when(dealerStatsManager.getProductActivationsStats(?, ?, ?, ?, ?)(?))
        .thenReturnF(dailyClientProductActivations)
      val res = manager
        .getProductActivationsStats(
          dealer,
          None,
          None,
          from,
          Some(to)
        )
        .futureValue
      res.getStatus shouldBe SUCCESS
      val totalStats = res.getTotalStats
      totalStats.getTotal.getSum shouldBe 289185
      totalStats.getTotal.getCount shouldBe 2238
      checkDailyStats(res.getDailyStatsList.asScala.toVector)
    }
  }

  "WalletManager.getOfferProductsActivationsTotalStats()" should {

    "get stats filtered by vin" in {
      forAll(carsOfferGen("1071117871-0a288"), carsOfferGen("1073188176-2b601371")) { (offer0, _) =>
        reset(offerLoader)
        when(dealerStatsManager.getOfferProductActivationsDailyStats(?, ?, ?, ?, ?)(?)).thenReturnF(offerMetrics)
        when(offerLoader.getListing(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturnF {
          OfferListingResponse
            .newBuilder()
            .addOffers(offer0)
            .setPagination {
              Pagination.newBuilder().setPage(1).setPageSize(2).setTotalOffersCount(2).setTotalPageCount(1)
            }
            .build()
        }
        val res = manager
          .getOfferProductsActivationsTotalStats(
            dealer,
            page1Params,
            OfferSorting.Spending,
            None,
            Some(offer0.getDocuments.getVin)
          )
          .futureValue
        res.getStatus shouldBe SUCCESS
        val stat0 = res.getOfferProductsActivationsStatsList.asScala.toList.head
        stat0.getOffer.id.toPlain shouldBe "1071117871-0a288"
        val stat0Boost = stat0.getProductStatsList.asScala.toList.head
        val stat0Placement = stat0.getProductStatsList.asScala.toList.tail.head
        stat0Boost.getProduct shouldBe "boost"
        stat0Boost.getCount shouldBe 1
        stat0Boost.getSum shouldBe 150
        stat0Placement.getProduct shouldBe "placement"
        stat0Placement.getCount shouldBe 2
        stat0Placement.getSum shouldBe 170
        stat0.getTotalStats.getCount shouldBe 3
        stat0.getTotalStats.getSum shouldBe 320
        verify(offerLoader).getListing(
          eq(CategorySelector.All),
          eq(dealer),
          eq(Paging(1, pageSize)),
          argThat[Filters] { filter =>
            filter.getOfferIRefList.asScala.isEmpty &&
            filter.getVinList.size() == 1 &&
            filter.getVinList.asScala.toSet == Set(offer0.getDocuments.getVin)
          },
          eq(NoSorting),
          includeRemoved = eq(true),
          eq(EnrichOptions(techParams = true)),
          eq(DecayOptions.ForOwner)
        )(?)
      }
    }

    "get stats sorted by total spending" in {
      forAll(carsOfferGen("1071117871-0a288"), carsOfferGen("1073188176-2b601371")) { (offer0, offer1) =>
        reset(offerLoader)
        when(dealerStatsManager.getOfferProductActivationsDailyStats(?, ?, ?, ?, ?)(?)).thenReturnF(offerMetrics)
        when(offerLoader.getListing(?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturnF {
          OfferListingResponse
            .newBuilder()
            .addOffers(offer0)
            .addOffers(offer1)
            .setPagination {
              Pagination.newBuilder().setPage(1).setPageSize(2).setTotalOffersCount(2).setTotalPageCount(1)
            }
            .build()
        }
        val res = manager
          .getOfferProductsActivationsTotalStats(
            dealer,
            page1Params,
            OfferSorting.Spending,
            None,
            None
          )
          .futureValue
        res.getStatus shouldBe SUCCESS
        val paging = res.getPaging
        paging.getTotal shouldBe offersListing.totalCount
        paging.getPageCount shouldBe 1
        val stat0 = res.getOfferProductsActivationsStatsList.asScala.toList.head
        val stat1 = res.getOfferProductsActivationsStatsList.asScala.toList.tail.head
        stat0.getOffer.id.toPlain shouldBe "1073188176-2b601371"
        stat1.getOffer.id.toPlain shouldBe "1071117871-0a288"
        val stat0Placement = stat0.getProductStatsList.asScala.toList.head
        val stat1Boost = stat1.getProductStatsList.asScala.toList.head
        val stat1Placement = stat1.getProductStatsList.asScala.toList.tail.head
        stat1Boost.getProduct shouldBe "boost"
        stat1Boost.getCount shouldBe 1
        stat1Boost.getSum shouldBe 150
        stat1Placement.getProduct shouldBe "placement"
        stat1Placement.getCount shouldBe 2
        stat1Placement.getSum shouldBe 170
        stat0Placement.getProduct shouldBe "placement"
        stat0Placement.getCount shouldBe 2
        stat0Placement.getSum shouldBe 260
        stat0.getTotalStats.getCount shouldBe 2
        stat0.getTotalStats.getSum shouldBe 260
        stat1.getTotalStats.getCount shouldBe 3
        stat1.getTotalStats.getSum shouldBe 320
        verify(offerLoader).getListing(
          eq(CategorySelector.All),
          eq(dealer),
          eq(Paging(1, pageSize)),
          argThat[Filters] { filter =>
            val iRefs = filter.getOfferIRefList.asScala
            iRefs.size == 2 && iRefs.map(_.longValue()).toSet == Set(1071117871L, 1073188176L)
          },
          eq(NoSorting),
          includeRemoved = eq(true),
          eq(EnrichOptions(techParams = true)),
          eq(DecayOptions.ForOwner)
        )(?)

      }
    }
  }
}
