package ru.auto.api.managers.tradein

import java.time.{LocalDate, OffsetDateTime}

import com.google.protobuf.Timestamp
import org.mockito.Mockito._
import ru.auto.api.ApiOfferModel.{Category, Offer}
import ru.auto.api.BaseSpec
import ru.auto.api.TradeInRequestsListingOuterClass.TradeInRequest.BillingStatus.NEW
import ru.auto.api.TradeInRequestsListingOuterClass.{SectionAvailable, TradeInRequest, UserInfo}
import ru.auto.api.managers.callback.PhoneCallbackManager
import ru.auto.api.managers.decay.{DecayManager, DecayOptions}
import ru.auto.api.managers.enrich.{EnrichManager, EnrichOptions}
import ru.auto.api.model.ModelGenerators.{DealerOfferGen, OfferGen}
import ru.auto.api.model._
import ru.auto.api.model.salesman.tradein.{SectionRecordsAvailable, TradeInCoreListingSalesmanResponse, TradeInRequestCore}
import ru.auto.api.model.salesman.{Page, Paging => PagingModel}
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.{ManagerUtils, Request, RequestImpl}
import ru.auto.cabinet.TradeInRequest.TradeInRequestForm
import ru.auto.cabinet.TradeInRequest.TradeInRequestForm.{ClientInfo => FormClientInfo, OfferInfo => FormOfferInfo, UserInfo => FormUserInfo}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class TradeInManagerSpec extends BaseSpec with MockitoSupport {

  private val salesmanClient = mock[SalesmanClient]
  private val vosClient = mock[VosClient]
  private val enrichManager = mock[EnrichManager]
  private val phoneCallbackManager = mock[PhoneCallbackManager]
  private val decayManager = mock[DecayManager]

  private val tradeInManager =
    new TradeInManager(
      salesmanClient,
      vosClient,
      enrichManager,
      phoneCallbackManager,
      decayManager
    )

  implicit val trace: Traced = Traced.empty

  implicit private val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r
  }

  private val dealerRef = AutoruDealer(1L)
  private val dealerId = dealerRef.clientId
  private val clientOfferId = "1084395495-b5fc34ea"

  private val clientOffer =
    DealerOfferGen.next.toBuilder
      .setId(clientOfferId)
      .setUserRef(dealerRef.toPlain)
      .build()

  private val userOffer = OfferGen.next
  private val userId = 123
  private val userName = "Alex"
  private val userPhone = "89175586932"
  private val billingCost = 10000L
  private val billingStatus = "NEW"
  private val createDate = OffsetDateTime.now()
  private val tradeInId = 1L

  private val sectionsAvailable =
    SectionRecordsAvailable(clientOffer.getSection.toString, available = true)

  private val salesmanResponse = TradeInCoreListingSalesmanResponse(
    Seq(
      TradeInRequestCore(
        tradeInId,
        dealerId,
        clientOffer.getId,
        Some(userId),
        userPhone,
        Some(userName),
        Some(userOffer.getId),
        billingCost,
        billingStatus,
        createDate
      )
    ),
    Seq(sectionsAvailable),
    billingCost,
    PagingModel(1, 1, Page(1, 1))
  )

  private val formClientInfo =
    FormClientInfo
      .newBuilder()
      .setClientId(dealerId)
      .build()

  private val formClientOfferInfo =
    FormOfferInfo
      .newBuilder()
      .setOfferId(clientOfferId)
      .setCategory(Category.CARS)
      .build()

  private val formUserInfo =
    FormUserInfo
      .newBuilder()
      .setPhoneNumber("+7 (917)-456-32-14")
      .build()

  private val form: TradeInRequestForm =
    TradeInRequestForm
      .newBuilder()
      .setClientInfo(formClientInfo)
      .setClientOfferInfo(formClientOfferInfo)
      .setUserInfo(formUserInfo)
      .build()

  "TradeInManager" should {

    "throw exception if unable to request salesman" in {
      reset(salesmanClient, vosClient)
      when(salesmanClient.getTradeInRequests(?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Future.failed(new IllegalStateException()))

      intercept[IllegalStateException](
        tradeInManager
          .getTradeInRequests(
            1,
            None,
            LocalDate.now(),
            None,
            Paging(1, 20)
          )
          .await
      )
      verifyNoMoreInteractions(vosClient)
    }

    "throw exception if unable to request vos for any offer" in {
      reset(salesmanClient, vosClient)
      when(salesmanClient.getTradeInRequests(?, ?, ?, ?, ?, ?)(?))
        .thenThrow(new IllegalStateException())

      intercept[IllegalStateException](
        tradeInManager
          .getTradeInRequests(
            1,
            None,
            LocalDate.now(),
            None,
            Paging(1, 20)
          )
          .await
      )
    }

    "throw exception if unable to enrich any offer" in {
      reset(salesmanClient, vosClient, enrichManager)

      when(salesmanClient.getTradeInRequests(?, ?, ?, ?, ?, ?)(?))
        .thenReturnF(salesmanResponse)
      when(vosClient.getOffer(?, eq(OfferID.parse(clientOffer.getId)), ?, ?, ?)(?))
        .thenReturnF(clientOffer)
      when(vosClient.getOffer(?, eq(OfferID.parse(userOffer.getId)), ?, ?, ?)(?))
        .thenReturnF(userOffer)
      when(enrichManager.enrich(any[Offer](), ?)(?))
        .thenThrow(new IllegalStateException())

      intercept[IllegalStateException](
        tradeInManager
          .getTradeInRequests(
            1,
            None,
            LocalDate.now(),
            None,
            Paging(1, 20)
          )
          .await
      )
    }

    "find trade-in requests" in {
      reset(salesmanClient, vosClient, enrichManager)

      when(salesmanClient.getTradeInRequests(?, ?, ?, ?, ?, ?)(?))
        .thenReturnF(salesmanResponse)
      when(vosClient.getOffer(?, eq(OfferID.parse(clientOffer.getId)), ?, ?, ?)(?))
        .thenReturnF(clientOffer)
      when(vosClient.getOffer(?, eq(OfferID.parse(userOffer.getId)), ?, ?, ?)(?))
        .thenReturnF(userOffer)
      when(enrichManager.enrich(clientOffer, EnrichOptions(techParams = true)))
        .thenReturnF(clientOffer)
      when(enrichManager.enrich(userOffer, EnrichOptions(techParams = true)))
        .thenReturnF(userOffer)
      when(decayManager.decay(userOffer, DecayOptions.full)(request))
        .thenReturnF(userOffer)
      when(decayManager.decay(clientOffer, DecayOptions.empty)(request))
        .thenReturnF(clientOffer)

      val expectedTradeInRequest = TradeInRequest
        .newBuilder()
        .setId(tradeInId)
        .setBillingCost(100)
        .setBillingStatus(NEW)
        .setCreateDate(Timestamp.newBuilder().setSeconds(createDate.toEpochSecond))
        .setClientOffer(clientOffer)
        .setUserOffer(userOffer)
        .setUserInfo(UserInfo.newBuilder().setPhoneNumber(userPhone).setName(userName).setUserId(userId))
        .build()

      val result = tradeInManager.getTradeInRequests(1, None, LocalDate.now(), None, Paging(1, 20)).futureValue

      val sectionsAvailable = result.getSectionsAvailableList.asScala
      sectionsAvailable.size shouldBe 1
      sectionsAvailable.head shouldBe SectionAvailable
        .newBuilder()
        .setSection(clientOffer.getSection)
        .setAvailable(true)
        .build()

      val totalCost = result.getTotalCost
      totalCost shouldBe 100

      val tradeInRequests = result.getTradeInRequestsList.asScala
      tradeInRequests.size shouldBe 1

      tradeInRequests.head shouldBe expectedTradeInRequest
    }

    "throw exception if unable to request salesmanApi" in {
      reset(salesmanClient, phoneCallbackManager)

      when(vosClient.getOffer(?, ?, ?, ?, ?)(?))
        .thenReturn(Future(clientOffer))

      when(salesmanClient.putTradeInRequest(?)(?))
        .thenReturn(Future.failed(new IllegalStateException()))

      intercept[IllegalStateException](tradeInManager.createTradeInRequest(form).await)
      verifyNoMoreInteractions(phoneCallbackManager)
    }

    "throw exception if unable to request callKeeperApi" in {
      reset(salesmanClient, phoneCallbackManager)
      when(vosClient.getOffer(?, ?, ?, ?, ?)(?))
        .thenReturn(Future(clientOffer))
      when(salesmanClient.putTradeInRequest(?)(?))
        .thenReturn(Future.unit)
      when(phoneCallbackManager.registerPhoneCallback(?, ?, ?, ?)(?))
        .thenReturn(Future.failed(new IllegalStateException()))

      intercept[IllegalStateException](tradeInManager.createTradeInRequest(form).await)
    }

    "create trade-in request and return SuccessResponse" in {
      reset(salesmanClient, phoneCallbackManager)
      when(vosClient.getOffer(?, ?, ?, ?, ?)(?))
        .thenReturn(Future(clientOffer))
      when(salesmanClient.putTradeInRequest(?)(?))
        .thenReturn(Future.unit)
      when(phoneCallbackManager.registerPhoneCallback(?, ?, ?, ?)(?))
        .thenReturn(Future(ManagerUtils.SuccessResponse))

      val result = tradeInManager.createTradeInRequest(form).await
      val expectedResult = ManagerUtils.SuccessResponse
      result shouldBe expectedResult
    }

    "put trade-in request with empty client_id field" in {
      reset(salesmanClient, vosClient, phoneCallbackManager)

      val formWithoutClientId = form.toBuilder

      formWithoutClientId.getClientInfoBuilder
        .clearClientId()

      when(vosClient.getOffer(?, ?, ?, ?, ?)(?))
        .thenReturn(Future(clientOffer))
      when(salesmanClient.putTradeInRequest(?)(?))
        .thenReturn(Future.unit)
      when(phoneCallbackManager.registerPhoneCallback(?, ?, ?, ?)(?))
        .thenReturn(Future(ManagerUtils.SuccessResponse))

      val result = tradeInManager.createTradeInRequest(formWithoutClientId.build()).await
      val expectedResult = ManagerUtils.SuccessResponse
      result shouldBe expectedResult

      verify(vosClient)
        .getOffer(eq(CategorySelector.Cars), eq(OfferID.parse(clientOfferId)), ?, ?, ?)(?)
    }

    "put trade-in request with empty client_id and client_offer_category field" in {
      reset(salesmanClient, vosClient, phoneCallbackManager)

      val formWithoutClientId = form.toBuilder

      formWithoutClientId.getClientInfoBuilder
        .clearClientId()

      formWithoutClientId.getClientOfferInfoBuilder
        .clearCategory()

      when(vosClient.getOffer(?, ?, ?, ?, ?)(?))
        .thenReturn(Future(clientOffer))
      when(salesmanClient.putTradeInRequest(?)(?))
        .thenReturn(Future.unit)
      when(phoneCallbackManager.registerPhoneCallback(?, ?, ?, ?)(?))
        .thenReturn(Future(ManagerUtils.SuccessResponse))

      val result = tradeInManager.createTradeInRequest(formWithoutClientId.build()).await
      val expectedResult = ManagerUtils.SuccessResponse
      result shouldBe expectedResult

      verify(vosClient)
        .getOffer(eq(CategorySelector.All), eq(OfferID.parse(clientOfferId)), ?, ?, ?)(?)
    }

    "remove wrong symbols from phone number and create trade-in request" in {
      reset(salesmanClient, phoneCallbackManager)
      when(vosClient.getOffer(?, ?, ?, ?, ?)(?))
        .thenReturn(Future(clientOffer))
      when(salesmanClient.putTradeInRequest(?)(?))
        .thenReturn(Future.unit)
      when(phoneCallbackManager.registerPhoneCallback(?, ?, ?, ?)(?))
        .thenReturn(Future(ManagerUtils.SuccessResponse))

      val expectedNumber = "+79174563214"

      val expectedForm = form.toBuilder
        .clone()
        .setUserInfo(
          form.getUserInfo.toBuilder.setPhoneNumber(
            expectedNumber
          )
        )
        .build()

      val result = tradeInManager.createTradeInRequest(form).await
      result shouldBe ManagerUtils.SuccessResponse

      verify(salesmanClient).putTradeInRequest(eq(expectedForm))(?)
    }
  }
}
