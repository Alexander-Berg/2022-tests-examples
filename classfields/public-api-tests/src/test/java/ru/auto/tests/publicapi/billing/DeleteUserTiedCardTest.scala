package ru.auto.tests.publicapi.billing

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_REQUEST, NO_AUTH}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.rules.CreditCardRule

import scala.annotation.meta.getter

@DisplayName("DELETE /billing/{salesmanDomain}/tied-cards")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteUserTiedCardTest {

  private val DOMAIN = "autoru"
  private val PAYMENT_SYSTEM_ID = "yandexkassa_v3"
  private val CARD_ID = "555555|4444"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @(Rule @getter)
  @Inject
  val creditCardRule: CreditCardRule = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val accountManager: AccountManager = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.billing.removeTiedCard()
      .salesmanDomainPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee401WithoutSessionId(): Unit = {
    val response = api.billing.removeTiedCard().reqSpec(defaultSpec)
      .salesmanDomainPath(DOMAIN)
      .paymentSystemIdQuery(PAYMENT_SYSTEM_ID)
      .cardIdQuery(CARD_ID)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(NO_AUTH).hasStatus(ERROR)
      .hasDetailedError("Expected private user. But AnonymousUser. Provide valid session_id")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidPaymentSystemId(): Unit = {
    val account = accountManager.create
    val sessionId = adaptor.login(account).getSession.getId
    val invalidPaymentSystemId = getRandomString

    val response = api.billing.removeTiedCard().reqSpec(defaultSpec)
      .salesmanDomainPath(DOMAIN)
      .paymentSystemIdQuery(invalidPaymentSystemId)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR)
      .hasDetailedError(s"No enum constant ru.yandex.vertis.banker.model.ApiModel.PaymentSystemId.${invalidPaymentSystemId.toUpperCase}")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidSalesmanDomain(): Unit = {
    val account = accountManager.create
    val sessionId = adaptor.login(account).getSession.getId
    val invalidSalesmanDomain = getRandomString

    val response = api.billing.removeTiedCard().reqSpec(defaultSpec)
      .salesmanDomainPath(invalidSalesmanDomain)
      .paymentSystemIdQuery(PAYMENT_SYSTEM_ID)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR)
    Assertions.assertThat(response.getDetailedError)
      .contains(s"Invalid salesman domain - ${invalidSalesmanDomain} provided")
  }

  @Test
  @Owner(TIMONDL)
  def shouldChangePreferredSettingToTrue(): Unit = {
    val account = accountManager.create
    val sessionId = adaptor.login(account).getSession.getId
    creditCardRule.addCreditCard(sessionId)

    api.billing.removeTiedCard().reqSpec(defaultSpec)
      .salesmanDomainPath(DOMAIN)
      .paymentSystemIdQuery(PAYMENT_SYSTEM_ID)
      .cardIdQuery(CARD_ID)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val currentUserInfo = adaptor.getUserInfo(sessionId)
    Assertions.assertThat(currentUserInfo.getTiedCards).isNullOrEmpty()
  }
}
