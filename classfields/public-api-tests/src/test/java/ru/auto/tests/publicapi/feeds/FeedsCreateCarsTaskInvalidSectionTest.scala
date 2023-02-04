package ru.auto.tests.publicapi.feeds

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.utils.AssertUtils.assertApiError
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_PARAMS_DETAILS, BAD_REQUEST}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.operations.feeds.tasks.defaultTaskSettings
import ru.auto.tests.publicapi.operations.feeds.tasks.cars.CreateCarsFeedsTaskOps
import ru.auto.tests.utils.Authorization

import scala.annotation.meta.getter

@DisplayName("POST /feeds/task/cars/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class FeedsCreateCarsTaskInvalidSectionTest extends CreateCarsFeedsTaskOps with Authorization {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  override val api: ApiClient = null

  @Inject
  override val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee400WhenInvalidCarsSection(): Unit = {
    val expectedError = "No enum constant ru.auto.api.ApiOfferModel.Section.INVALID_SECTION"
    assertApiError(BAD_REQUEST, Some(expectedError)) {
      createCarsTask(defaultTaskSettings, section = "INVALID_SECTION")
        .xSessionIdHeader(getSessionId(Accounts.TestAccount))
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee400WhenUnknownCarSection(): Unit = {
    val expectedError = "Unknown section: [SECTION_UNKNOWN]. Known values: USED, NEW"
    assertApiError(BAD_PARAMS_DETAILS, Some(expectedError)) {
      createCarsTask(defaultTaskSettings, section = "SECTION_UNKNOWN")
        .xSessionIdHeader(getSessionId(Accounts.TestAccount))
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee404WhenMissingCarSection(): Unit = {
    assertApiError(BAD_REQUEST) {
      createCarsTask(defaultTaskSettings, section = "")
        .xSessionIdHeader(getSessionId(Accounts.TestAccount))
        .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
    }
  }

}
