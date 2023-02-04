package ru.auto.tests.publicapi.dealer.whitelist

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.{assertThat => modelsAssertThat}
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.testdata.WhitelistDealerAccounts._
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.CUSTOMER_ACCESS_FORBIDDEN
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.{getDemoAccount, getManagerAccount}

import scala.annotation.meta.getter

@DisplayName("POST /dealer/phones/whitelist/available")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class AvailableWhitelistTest {

  private val dealerAvailable = "20101"
  private val dealerNotAvailable = "16453"

  @(Rule @getter)
  @Inject
  var defaultRules: RuleChain = null

  @Inject
  private var api: ApiClient = null

  @Inject
  private var adaptor: PublicApiAdaptor = null

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessManagerSession(): Unit = {
    val managerSessionId = adaptor.login(getManagerAccount).getSession.getId

    val whitelistAvailable = api
      .dealer()
      .availableDealerPhonesToWhiteList()
      .reqSpec(defaultSpec)
      .xDealerIdHeader(dealerAvailable)
      .xSessionIdHeader(managerSessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(whitelistAvailable).isTrue
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessDealerSession(): Unit = {
    val managerSessionId = adaptor.login(getDemoAccount).getSession.getId

    val whitelistAvailable = api
      .dealer()
      .availableDealerPhonesToWhiteList()
      .reqSpec(defaultSpec)
      .xDealerIdHeader(dealerAvailable)
      .xSessionIdHeader(managerSessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(whitelistAvailable).isTrue
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccessManagerSessionIsFalse(): Unit = {
    val managerSessionId = adaptor.login(getManagerAccount).getSession.getId

    val whitelistAvailable = api
      .dealer()
      .availableDealerPhonesToWhiteList()
      .reqSpec(defaultSpec)
      .xDealerIdHeader(dealerNotAvailable)
      .xSessionIdHeader(managerSessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(whitelistAvailable).isFalse
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenNoAuth(): Unit = {
    api
      .dealer()
      .availableDealerPhonesToWhiteList()
      .xDealerIdHeader(dealerAvailable)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WithoutSessionId(): Unit = {
    val response = api
      .dealer()
      .availableDealerPhonesToWhiteList()
      .reqSpec(defaultSpec)
      .xDealerIdHeader(dealerAvailable)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])

    modelsAssertThat(response).hasStatus(ERROR).hasError(CUSTOMER_ACCESS_FORBIDDEN)
    Assertions
      .assertThat(response.getDetailedError)
      .contains("Permission denied to SETTINGS:Read for anon")
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WithoutDealerId(): Unit = {
    val managerSessionId = adaptor.login(getManagerAccount).getSession.getId

    val response = api.dealer
      .availableDealerPhonesToWhiteList()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(managerSessionId)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])

    modelsAssertThat(response).hasStatus(ERROR).hasError(CUSTOMER_ACCESS_FORBIDDEN)
    Assertions
      .assertThat(response.getDetailedError)
      .contains("Permission denied to SETTINGS:Read for user:")
  }

}
