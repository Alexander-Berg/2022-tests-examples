package ru.auto.tests.publicapi.feeds

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.utils.AssertUtils.assertApiError
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.operations.feeds.tasks.defaultTaskSettings
import ru.auto.tests.publicapi.operations.feeds.tasks.moto.CreateMotoFeedsTaskOps
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.MotoCategoryEnum
import ru.auto.tests.utils.Authorization

import scala.annotation.meta.getter

@DisplayName("POST /feeds/task/moto/{moto_category}/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class FeedsCreateMotoTaskInvalidSectionTest(category: MotoCategoryEnum) extends CreateMotoFeedsTaskOps with Authorization {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  override val api: ApiClient = null

  @Inject
  override val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee400WhenInvalidMotoSection(): Unit = {
    val expectedError = "No enum constant ru.auto.api.ApiOfferModel.Section.INVALID_SECTION"
    assertApiError(BAD_REQUEST, Some(expectedError)) {
      createMotoTask(defaultTaskSettings, category.name, section = "INVALID_SECTION")
        .xSessionIdHeader(getSessionId(Accounts.TestAccount))
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
    }
  }

}

object FeedsCreateMotoTaskInvalidSectionTest {
  @Parameterized.Parameters(name = "category = {0}")
  def parameters = MotoCategoryEnum.values()
}
