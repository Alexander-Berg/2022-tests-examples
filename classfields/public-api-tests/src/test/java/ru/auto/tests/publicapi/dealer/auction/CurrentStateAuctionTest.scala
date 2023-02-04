package ru.auto.tests.publicapi.dealer.auction

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_UNAUTHORIZED}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.publicapi.consts.DealerConsts.{CHERY_BONUS_MODEL, CHERY_MARK, CHERY_OFFER_PATH}
import ru.auto.tests.publicapi.consts.Owners.MONEY_DUTY
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getAutoAbsolutionAccount

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters.CollectionHasAsScala

@DisplayName("GET /dealer/auction/current-state")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
@Owner(MONEY_DUTY)
class CurrentStateAuctionTest extends AuctionBaseTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.dealer.currentState().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee401WithoutSession(): Unit = {
    val response = api.dealer
      .currentState()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response)
      .hasStatus(ERROR)
      .hasError(NO_AUTH)
      .hasDetailedError("Expected dealer user. Provide valid session_id")
  }

  @Test
  def shouldSee200CurrentState(): Unit = {
    val sessionId = adaptor.login(getAutoAbsolutionAccount).getSession.getId

    adaptor.createDealerOffer(sessionId, CARS, CHERY_OFFER_PATH)

    val currentState = api.dealer.currentState
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val stateByContext = currentState.getStates.asScala.find(state =>
      (state.getContext.getMarkCode == CHERY_MARK
        && state.getContext.getModelCode == CHERY_BONUS_MODEL)
    )

    assertThat(stateByContext.get.getContext).hasMarkCode(CHERY_MARK)
    assertThat(stateByContext.get.getContext).hasModelCode(CHERY_BONUS_MODEL)
  }
}
