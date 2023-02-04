package ru.auto.tests.orders

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.MatcherAssert
import org.junit.Assert.{assertEquals, assertNull}
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.model.AutoApiVinOrdersCreateOrderRequest._
import ru.auto.tests.model.{AutoApiVinOrdersCreateOrderRequest, AutoApiVinOrdersCreateOrderResponse, AutoApiVinOrdersGetOrdersListRequest, AutoApiVinOrdersGetOrdersListRequestOrdersFilter}
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

@DisplayName("GET /orders")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class OrdersTest {

  val VIN = "X4XKC81180C572568"
  val User = "dealer:20101"

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  def shouldNotSeeDiffWithProductionPaymentFailedOrder(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient
        .orders()
        .getOrderStatus
        .reqSpec(defaultSpec)
        .orderIdQuery("a26674db-924d-4dad-a9d0-5c7a7713f092")
        .userIdQuery("dealer:37444")
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi))
    )
  }

  @Test
  def shouldNotSeeDiffWithProductionSuccessOrder(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient
        .orders()
        .getOrderStatus
        .reqSpec(defaultSpec)
        .orderIdQuery("4d6f3f88-1a4a-4af0-9669-88e2f23262da")
        .userIdQuery("dealer:20101")
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi))
    )
  }

  @Test
  def shouldSeeCreatedOrder(): Unit = {

    val createdOrder: AutoApiVinOrdersCreateOrderResponse = api
      .orders()
      .createOrder()
      .reqSpec(defaultSpec)
      .body(
        new AutoApiVinOrdersCreateOrderRequest()
          .identifier(VIN)
          .identifierType(IdentifierTypeEnum.VIN)
          .reportType(ReportTypeEnum.GIBDD_REPORT)
          .userId(User)
      )
      .executeAs(validatedWith(shouldBe200OkJSON))

    val getOrderResponse = api
      .orders()
      .getOrderStatus
      .orderIdQuery(createdOrder.getOrder.getId)
      .userIdQuery(User)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertEquals(createdOrder.getOrder, getOrderResponse.getOrder)
  }

  @Test
  def shouldNotSeeDiffWithProductionOrdersByVin(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient
        .orders()
        .getOrdersList
        .reqSpec(defaultSpec)
        .body(
          new AutoApiVinOrdersGetOrdersListRequest().filter(
            new AutoApiVinOrdersGetOrdersListRequestOrdersFilter().vin(VIN)
          )
        )
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi))
    )
  }

}
