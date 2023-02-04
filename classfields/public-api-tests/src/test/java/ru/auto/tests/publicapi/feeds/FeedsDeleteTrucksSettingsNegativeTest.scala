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
import ru.auto.tests.publicapi.operations.feeds.settings.trucks.DeleteTrucksFeedsSettingsOps
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{AUTH_ERROR, CUSTOMER_ACCESS_FORBIDDEN}
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{SectionEnum, TruckCategoryEnum}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.utils.AssertUtils.assertApiError
import ru.auto.tests.utils.Authorization
import ru.auto.tests.utils.ParameterizedUtils.parameterize

import scala.annotation.meta.getter

@DisplayName("DELETE /feeds/settings/trucks/{trucks_category}/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class FeedsDeleteTrucksSettingsNegativeTest(category: TruckCategoryEnum, section: SectionEnum) extends DeleteTrucksFeedsSettingsOps with Authorization {

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
    deleteTrucksSettings(category, section)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee401WithUserSession(): Unit = {
    val sessionId = getSessionId(accountManager.create())

    assertApiError(AUTH_ERROR, Some(AUTH_ERROR.getValue)) {
      deleteTrucksSettings(category, section)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee401WhenNoAuth(): Unit = {
    deleteTrucksSettingsWithoutAuth(category, section)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenNoAclResource(): Unit = {
    val dealer = Accounts.NoFeedsAclTestAccount
    val expectedError = s"Permission denied to FEEDS:ReadWrite for user:${dealer.getId}"

    assertApiError(CUSTOMER_ACCESS_FORBIDDEN, Some(expectedError)) {
      deleteTrucksSettings(category, section)
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
      deleteTrucksSettings(category, section)
        .xSessionIdHeader(getSessionId(dealer))
        .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
    }
  }

}

object FeedsDeleteTrucksSettingsNegativeTest {
  @Parameterized.Parameters(name = "category = {0}, section = {1}")
  def parameters = parameterize {
    for {
      category <- TruckCategoryEnum.values()
      section <- SectionEnum.values()
    } yield (category, section)
  }
}


