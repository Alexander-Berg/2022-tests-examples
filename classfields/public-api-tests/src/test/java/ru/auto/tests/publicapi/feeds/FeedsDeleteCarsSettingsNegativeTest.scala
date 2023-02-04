package ru.auto.tests.publicapi.feeds

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_UNAUTHORIZED}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.feeds.Accounts
import ru.auto.tests.publicapi.operations.feeds.settings.cars.DeleteCarsFeedsSettingsOps
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{AUTH_ERROR, CUSTOMER_ACCESS_FORBIDDEN}
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.SectionEnum
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.utils.AssertUtils.assertApiError
import ru.auto.tests.utils.Authorization

import scala.annotation.meta.getter

@DisplayName("DELETE /feeds/settings/cars/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class FeedsDeleteCarsSettingsNegativeTest(section: SectionEnum) extends DeleteCarsFeedsSettingsOps with Authorization {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  override val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  override val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee401WithoutSession(): Unit = {
    deleteCarsSettings(section)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee401WithUserSession(): Unit = {
    val sessionId = getSessionId(accountManager.create())

    assertApiError(AUTH_ERROR, Some(AUTH_ERROR.getValue)) {
      deleteCarsSettings(section)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenNoAuth(): Unit = {
    deleteCarsSettingsWithoutAuth(section)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenNoAclResource(): Unit = {
    val dealer = Accounts.NoFeedsAclTestAccount
    val expectedError = s"Permission denied to FEEDS:ReadWrite for user:${dealer.getId}"

    assertApiError(CUSTOMER_ACCESS_FORBIDDEN, Some(expectedError)) {
      deleteCarsSettings(section)
        .xSessionIdHeader(getSessionId(dealer))
        .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenReadOnlyAclResource(): Unit = {
    val dealer = Accounts.ReadOnlyTestAccount
    val expectedError = s"Permission denied to FEEDS:ReadWrite for user:${dealer.getId}"

    assertApiError(CUSTOMER_ACCESS_FORBIDDEN, Some(expectedError)) {
      deleteCarsSettings(section)
        .xSessionIdHeader(getSessionId(dealer))
        .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
    }
  }

}

object FeedsDeleteCarsSettingsNegativeTest {
  @Parameterized.Parameters(name = "section = {0}")
  def parameters = SectionEnum.values()
}


