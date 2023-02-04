package ru.auto.api.managers.carfax

import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.{RawVinReportResponse, ResponseStatus}
import ru.auto.api.exceptions.{VinInvalid, VinNotFound}
import ru.auto.api.managers.carfax.CarfaxWalletManager.BoughtReportsFilter
import ru.auto.api.managers.carfax.offer.CarfaxOfferReportManager
import ru.auto.api.managers.carfax.orders.CarfaxOrdersManager
import ru.auto.api.managers.carfax.report.CarfaxReportManager
import ru.auto.api.model.ModelGenerators.{DealerUserRefGen, PrivateUserRefGen}
import ru.auto.api.model.{CategorySelector, OfferID, Paging, RequestParams}
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.vin.VinReportModel.{Header, RawVinReport, ReportOfferInfo => CarfaxOfferInfo}
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.auto.salesman.model.user.ApiModel.{VinHistoryBoughtReport, VinHistoryBoughtReports}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class CafaxBoughtReportsManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport {

  private val walletManager = mock[CarfaxWalletManager]
  private val reportManager = mock[CarfaxReportManager[RawVinReportResponse]]
  private val offerReportManager = mock[CarfaxOfferReportManager[RawVinReportResponse]]
  private val favoritesManager = mock[FavoriteManagerWrapper]
  private val ordersManager = mock[CarfaxOrdersManager]

  val manager =
    new CarfaxBoughtReportsManager(walletManager, reportManager, offerReportManager, favoritesManager, ordersManager)

  private val DefaultUser = PrivateUserRefGen.next.asRegistered
  private val DefaultDealerUser = DealerUserRefGen.next.asDealer
  private val DefaultFilter = BoughtReportsFilter(None, None, None, None, onlyActive = true)

  implicit private val defaultRequest: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(DefaultUser)
    r
  }

  private val defaultDealerRequest: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(DefaultUser)
    r.setDealer(DefaultDealerUser)
    r
  }

  before {
    reset(walletManager, reportManager, offerReportManager, favoritesManager)
  }

  "get bought reports" should {
    "return empty response" when {
      "no bought reports" in {
        when(walletManager.getBoughtReports(?, ?, ?)(?)).thenReturn(
          Future.successful(
            VinHistoryBoughtReports.newBuilder().build()
          )
        )
        when(favoritesManager.checkIfOffersAreInFavorite(?, ?, ?)(?)).thenReturn(
          Future.successful(Map.empty[OfferID, Boolean])
        )

        val res =
          manager.listBySalesmanPurchases(DefaultFilter, Paging.Default).await

        res.getReportsCount shouldBe 0
        res.getStatus shouldBe ResponseStatus.SUCCESS

        verify(walletManager, times(1)).getBoughtReports(?, ?, ?)(?)
        verify(reportManager, never()).getReport(?, ?, ?, ?)(?)
        verify(offerReportManager, never()).getReport(?, ?, ?, ?)(?)
      }
      "all reports response with non fatal error" in {
        val vin1 = "Z8T4C5S19BM005269"
        val vin2 = "Z8T4C5S19BM005270"
        val offerId2 = OfferID.parse("123-abc")

        when(walletManager.getBoughtReports(?, ?, ?)(?)).thenReturn(
          Future.successful(
            boughtReports(List((vin1, None), (vin2, Some(offerId2.toPlain))))
          )
        )

        when(reportManager.getReport(eq(vin1), decrementQuota = eq(false), forBoughtListing = eq(true), ?)(?))
          .thenReturn(Future.failed(VinNotFound(vin1)))
        when(
          offerReportManager
            .getReport(CategorySelector.Cars, offerId2, decrementQuota = false, forBoughtListing = true)
        ).thenReturn(Future.failed(VinInvalid(vin2)))
        when(favoritesManager.checkIfOffersAreInFavorite(?, ?, ?)(?)).thenReturn(
          Future.successful(Map.empty[OfferID, Boolean])
        )

        val res =
          manager.listBySalesmanPurchases(DefaultFilter, Paging.Default).await

        res.getReportsCount shouldBe 0
        res.getStatus shouldBe ResponseStatus.SUCCESS

        verify(walletManager, times(1)).getBoughtReports(?, ?, ?)(?)
        verify(reportManager, times(1)).getReport(eq(vin1), eq(false), eq(true), ?)(?)
        verify(offerReportManager, times(1)).getReport(
          eq(CategorySelector.Cars),
          eq(offerId2),
          eq(false),
          eq(true)
        )(?)
      }
    }
    "throw error" when {
      "salesman response with error" in {
        when(walletManager.getBoughtReports(?, ?, ?)(?)).thenReturn(
          Future.failed(new RuntimeException("some error"))
        )

        intercept[RuntimeException] {
          manager.listBySalesmanPurchases(DefaultFilter, Paging.Default).await
        }

        verify(walletManager, times(1)).getBoughtReports(?, ?, ?)(?)
        verify(favoritesManager, never).checkIfOffersAreInFavorite(?, ?, ?)(?)
        verify(reportManager, never()).getReport(?, ?, ?, ?)(?)
        verify(offerReportManager, never()).getReport(?, ?, ?, ?)(?)
      }
      "some report response with fatal error" in {
        val vin1 = "Z8T4C5S19BM005269"

        when(walletManager.getBoughtReports(?, ?, ?)(?)).thenReturn(
          Future.successful(boughtReports(List((vin1, None))))
        )
        when(favoritesManager.checkIfOffersAreInFavorite(?, ?, ?)(?)).thenReturn(
          Future.successful(Map.empty[OfferID, Boolean])
        )

        when(reportManager.getReport(eq(vin1), decrementQuota = eq(false), forBoughtListing = eq(true), ?)(?))
          .thenReturn(Future.failed(new RuntimeException("some error")))

        intercept[RuntimeException] {
          manager.listBySalesmanPurchases(DefaultFilter, Paging.Default).await
        }

        verify(walletManager, times(1)).getBoughtReports(?, ?, ?)(?)
        verify(reportManager, times(1)).getReport(eq(vin1), eq(false), eq(true), ?)(?)
        verify(offerReportManager, never()).getReport(?, ?, ?, ?)(?)
      }
    }
    "return list of bought response" when {
      "there are bought response and reports response with success" in {

        def checkFor(r: Request): Unit = {
          reset(walletManager, reportManager, offerReportManager, favoritesManager)
          val vin1 = "Z8T4C5S19BM005269"
          val offerId = OfferID(123, None)

          val reportResponse = {
            val responseBuilder = RawVinReportResponse.newBuilder()
            val reportBuilder = RawVinReport.newBuilder()
            reportBuilder
              .setVin(vin1)
              .setHeader(Header.getDefaultInstance)
              .setReportOfferInfo(CarfaxOfferInfo.newBuilder.setOfferId(offerId.toString))

            responseBuilder.setReport(reportBuilder).build()
          }

          when(walletManager.getBoughtReports(?, ?, ?)(?)).thenReturn(
            Future.successful(
              boughtReports(List((vin1, Some(offerId.toString))))
            )
          )
          when(favoritesManager.checkIfOffersAreInFavorite(?, ?, ?)(?)).thenReturn(
            Future.successful(Map(offerId -> true))
          )

          when(
            offerReportManager.getReport(
              eq(CategorySelector.Cars),
              eq(offerId),
              decrementQuota = eq(false),
              forBoughtListing = eq(true)
            )(?)
          ).thenReturn(Future.successful(reportResponse))

          val res = manager.listBySalesmanPurchases(DefaultFilter, Paging.Default)(r).await

          res.getReportsCount shouldBe 1
          res.getStatus shouldBe ResponseStatus.SUCCESS

          val resReport = res.getReports(0).getRawReport
          resReport.getVin shouldBe reportResponse.getReport.getVin
          resReport.getReportOfferInfo.getIsFavorite shouldBe true

          verify(walletManager, times(1)).getBoughtReports(?, ?, ?)(?)
          verify(offerReportManager, times(1)).getReport(
            eq(CategorySelector.Cars),
            eq(offerId),
            eq(false),
            eq(true)
          )(?)
          verify(reportManager, never()).getReport(?, ?, ?, ?)(?)
        }

        checkFor(defaultRequest)
        checkFor(defaultDealerRequest)
      }
    }
  }

  private def boughtReports(reports: List[(String, Option[String])]): VinHistoryBoughtReports = {
    val b = VinHistoryBoughtReports.newBuilder

    reports.foreach {
      case (vin, optOfferId) => b.addReports(boughtReport(vin, optOfferId))
    }

    b.build()
  }

  private def boughtReport(vin: String, optOfferId: Option[String]): VinHistoryBoughtReport = {
    val b = VinHistoryBoughtReport.newBuilder
    b.setVin(vin)
    optOfferId.foreach(b.setOfferId)
    b.build()
  }
}
