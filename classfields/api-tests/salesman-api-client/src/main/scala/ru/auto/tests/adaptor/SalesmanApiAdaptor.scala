package ru.auto.tests.adaptor

import java.time.OffsetDateTime

import com.google.common.collect.Lists.newArrayList
import com.google.gson.JsonArray
import com.google.inject.{AbstractModule, Inject, Singleton}
import io.qameta.allure.Step
import io.restassured.response.Response
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{
  shouldBe200Ok,
  shouldBe200OkJSON
}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.model.AutostrategyIdView.AutostrategyTypeEnum.ALWAYS_AT_FIRST_PAGE
import ru.auto.tests.model.ClientOfferInfoView.CategoryEnum.CARS
import ru.auto.tests.model.ClientOfferInfoView.SectionEnum.USED
import ru.auto.tests.model.{
  AlwaysAtFirstPagePayloadView,
  AutostrategyIdView,
  AutostrategyView,
  ClientInfoView,
  ClientOfferInfoView,
  OfferDescriptionView,
  TradeInRequestForm,
  UserInfoView,
  UserOfferInfoView
}
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

@Singleton
class SalesmanApiAdaptor extends AbstractModule {

  @Inject
  @Prod
  private val api: ApiClient = null

  @Step("Создаем запрос на Ads для клиента {clientId} с типом {requestType}")
  def createAdsRequest(clientId: Int, requestType: String): Unit =
    api.ads
      .insertAdsRequestRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(clientId)
      .requestPath(requestType)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))

  @Step("Получаем Ads для клиента {clientId} с типом {requestType}")
  def getAdsRequest(clientId: Int, requestType: String): Response =
    api.ads.getAdsRequestRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(clientId)
      .requestPath(requestType)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200Ok))

  @Step("Создаем запрос на трейд-ин для клиента {clientId}")
  def createTradeInRequest(clientId: Long): Unit = {
    val userInfo = new UserInfoView()
      .userId(50746400L)
      .name(getRandomString)
      .phoneNumber(getRandomString)
    val clientInfo = new ClientInfoView().clientId(clientId)
    val description = new OfferDescriptionView()
      .mark(getRandomString)
      .model(getRandomString)
      .year(2010L)
      .mileage(100000L)
      .price(4000000L)
    val clientOfferInfo = new ClientOfferInfoView()
      .offerId("1096541362-f822e368")
      .category(CARS)
      .section(USED)
      .description(description)
    val userOfferInfo = new UserOfferInfoView()
      .offerId("1096538572-894253c9")
      .category("CARS")
      .description(description)

    api.tradeIn
      .insertTradeInRequestRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(clientId)
      .body(
        new TradeInRequestForm()
          .clientInfo(clientInfo)
          .userInfo(userInfo)
          .clientOfferInfo(clientOfferInfo)
          .userOfferInfo(userOfferInfo)
      )
      .execute(validatedWith(shouldBe200OkJSON))
  }

  @Step("Создаем автостратегию для оффера {offerId}")
  def createAutostrategies(offerId: String): Unit = {
    val alwaysAtFirstPage = new AlwaysAtFirstPagePayloadView()
      .forMarkModelListing(true)
      .forMarkModelGenerationListing(true)
    val autostrategy = new AutostrategyView()
      .offerId(offerId)
      .fromDate(OffsetDateTime.now.toLocalDate)
      .toDate(OffsetDateTime.now.plusDays(1).toLocalDate)
      .maxApplicationsPerDay(1)
      .alwaysAtFirstPage(alwaysAtFirstPage)

    api.autostrategies.putAutostrategiesRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .body(newArrayList(autostrategy))
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
  }

  @Step("Получаем автостратегии для оффера {offerId}")
  def getAutostrategies(offerId: String): JsonArray =
    api.autostrategies.getOffersAutostrategiesRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .offerIdQuery(offerId)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonArray])

  @Step("Удаляем автостратегии для оффера {offerId}")
  def deleteAutostrategies(offerId: String): Unit =
    api.autostrategies
      .deleteAutostrategiesRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .body(
        newArrayList(
          new AutostrategyIdView()
            .offerId(offerId)
            .autostrategyType(ALWAYS_AT_FIRST_PAGE)
        )
      )
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))

  override protected def configure(): Unit = {}

}
