package ru.auto.api.managers.auction

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.auction.CallAuction.{AuctionAutoStrategySettings, CallAuctionRequestContext, ChangeAuctionAutoStrategyRequest, DeleteAuctionAutoStrategyRequest, MaxPositionForPrice}
import ru.auto.api.model.{AutoruDealer, DealerUserRoles, RequestParams, UserInfo, VarityResolution}
import ru.auto.api.services.auction.AuctionAutoStrategyClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.mockito.MockitoSupport
import vsmoney.auction.CommonModel.{AuctionContext, CriteriaValue}

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class CallAuctionAutoStrategyManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  val auctionAutoStrategyClientMock = mock[AuctionAutoStrategyClient]
  val auctionProtoConverterMock = mock[AuctionProtoConverter]

  private val DefaultUser = AutoruDealer(123)

  private val userInfo = UserInfo(
    ip = "1.1.1.1",
    deviceUid = None,
    optSessionID = None,
    session = None,
    varityResolution = VarityResolution.Human,
    DefaultUser,
    dealerRef = None,
    dealerUserRole = DealerUserRoles.Unknown
  )

  implicit private val defaultRequest: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(DefaultUser)
    r
  }

  val auctionAutoStrategyManager = new CallAuctionAutoStrategyManager(
    auctionAutoStrategyClientMock,
    auctionProtoConverterMock
  )

  "CallAuctionAutoStrategyManager" should {
    "should change request to auction auto strategy client" in {
      val request = ChangeAuctionAutoStrategyRequest
        .newBuilder()
        .setContext(
          auctionFrontContext
        )
        .setAutoStrategy(
          AuctionAutoStrategySettings
            .newBuilder()
            .setMaxBid(100L)
            .setMaxPositionForPrice(MaxPositionForPrice.newBuilder().build())
            .build()
        )
        .build()

      when(auctionProtoConverterMock.frontToBackContext(eq(request.getContext), eq(DefaultUser))(?))
        .thenReturnF(auctionBackContext)

      when(auctionAutoStrategyClientMock.create(?)(?))
        .thenReturn(Future.unit)

      auctionAutoStrategyManager.change(user = userInfo, changeRequest = request).await
    }
    "should remove request to auction auto strategy client" in {
      val request = DeleteAuctionAutoStrategyRequest
        .newBuilder()
        .setContext(auctionFrontContext)
        .build()

      when(auctionProtoConverterMock.frontToBackContext(eq(request.getContext), eq(DefaultUser))(?))
        .thenReturnF(auctionBackContext)

      when(auctionAutoStrategyClientMock.remove(?)(?))
        .thenReturn(Future.unit)

      auctionAutoStrategyManager.delete(user = userInfo, deleteRequest = request).await
    }
  }

  private val auctionFrontContext = CallAuctionRequestContext
    .newBuilder()
    .setMarkCode("BMW")
    .setModelCode("X6")
    .build()

  private val auctionBackContext = {
    val criteriaValues = List(
      CriteriaValue.newBuilder().setKey(CallAuctionManager.RegionCriteriaName).setValue("42").build(),
      CriteriaValue.newBuilder().setKey(CallAuctionManager.MarkCriteriaName).setValue("BMW").build(),
      CriteriaValue.newBuilder().setKey(CallAuctionManager.ModelCriteriaName).setValue("X6").build()
    )

    val criteriaContext = AuctionContext.CriteriaContext
      .newBuilder()
      .addAllCriteriaValues(criteriaValues.asJava)
      .build()

    AuctionContext
      .newBuilder()
      .setCriteriaContext(criteriaContext)
      .build()
  }
}
