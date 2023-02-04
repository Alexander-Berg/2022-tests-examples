package ru.auto.tests.offers

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.assertj.core.api.Assertions.assertThat
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.SERVICE
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName(
  "GET /service/{service}/offers/category/{offerCategory}/{offerId}/{productId}"
)
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[SalesmanApiModule]))
class GetPlacementDayNegativeTest {

  private val OFFER_ID = "1088989228-50a62064"
  private val PRODUCT = "premium"
  private val CATEGORY = "cars"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidCategory(): Unit =
    api.offers.getPlacementDay
      .reqSpec(defaultSpec)
      .offerCategoryPath(getRandomString)
      .offerIdPath(OFFER_ID)
      .productIdPath(PRODUCT)
      .servicePath(SERVICE)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidService(): Unit =
    api.offers.getPlacementDay
      .reqSpec(defaultSpec)
      .offerCategoryPath(CATEGORY)
      .offerIdPath(OFFER_ID)
      .productIdPath(PRODUCT)
      .servicePath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidProduct(): Unit = {
    val response = api.offers.getPlacementDay
      .reqSpec(defaultSpec)
      .offerCategoryPath(CATEGORY)
      .offerIdPath(OFFER_ID)
      .productIdPath(getRandomString)
      .servicePath(SERVICE)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .asString

    assertThat(response).isEqualTo("The requested resource could not be found.")
  }
}
