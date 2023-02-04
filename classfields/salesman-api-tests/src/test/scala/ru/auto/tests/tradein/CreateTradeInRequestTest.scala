package ru.auto.tests.tradein

import java.time.OffsetDateTime

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.model.ClientOfferInfoView.CategoryEnum.CARS
import ru.auto.tests.model.ClientOfferInfoView.SectionEnum.USED
import ru.auto.tests.model._
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /service/{service}/trade-in/client/{clientId}")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[SalesmanApiModule]))
class CreateTradeInRequestTest {

  private val CLIENT_ID = 20101

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee200InsertTradeInRequestRoute(): Unit = {
    val userName = getRandomString
    val userPhone = getRandomString
    val clientInfo = new ClientInfoView().clientId(20101L)
    val userInfo =
      new UserInfoView().userId(50746400L).name(userName).phoneNumber(userPhone)
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
      .clientIdPath(CLIENT_ID)
      .body(
        new TradeInRequestForm()
          .clientInfo(clientInfo)
          .userInfo(userInfo)
          .clientOfferInfo(clientOfferInfo)
          .userOfferInfo(userOfferInfo)
      )
      .execute(validatedWith(shouldBe200OkJSON))

    val response = api.tradeIn.getTradeInRequestsRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .fromQuery(OffsetDateTime.now.toLocalDate.toString)
      .pageNumQuery(1)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    val userNameResponse = response
      .getAsJsonArray("requests")
      .get(0)
      .getAsJsonObject
      .get("userName")
      .getAsString
    val userPhoneResponse = response
      .getAsJsonArray("requests")
      .get(0)
      .getAsJsonObject
      .get("userPhone")
      .getAsString
    assertThat(userNameResponse).isEqualTo(userName)
    assertThat(userPhoneResponse).isEqualTo(userPhone)
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400InsertTradeInRequestRoute(): Unit =
    api.tradeIn
      .insertTradeInRequestRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
}
