package ru.auto.tests.goods

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.common.collect.Lists.newArrayList
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.model.GoodsRequest
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe400WithMissedSalesmanUser

import scala.annotation.meta.getter

@DisplayName("POST /service/{service}/goods/client/{client}")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[SalesmanApiModule]))
class AddGoodsTest {

  private val CLIENT_ID = 20101
  private val INVALID_CLIENT = 0

  private val REQUEST = new GoodsRequest()
    .offerId("1095858148-3a897e9c")
    .category(CARS.getValue.toLowerCase)
    .section("used")
    .product("all_sale_fresh")

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee200AddGoodsRoute(): Unit =
    api.goods
      .addGoodsRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientPath(CLIENT_ID)
      .body(newArrayList(REQUEST))
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit =
    api.goods
      .addGoodsRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientPath(CLIENT_ID)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutSalesmanUserHeader(): Unit =
    api.goods
      .addGoodsRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientPath(CLIENT_ID)
      .body(newArrayList(REQUEST))
      .execute(validatedWith(shouldBe400WithMissedSalesmanUser))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidClientId(): Unit =
    api.goods
      .addGoodsRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientPath(INVALID_CLIENT)
      .body(newArrayList(REQUEST))
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(404)))
}
