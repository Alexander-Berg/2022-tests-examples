package ru.auto.tests.publicapi.dealer.auction

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
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.consts.DealerConsts.{CHERY_BONUS_MODEL, CHERY_MARK, CHERY_OFFER_PATH, DEFAULT_BID}
import ru.auto.tests.publicapi.consts.Owners.MONEY_DUTY
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_REQUEST, NO_AUTH}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getAutoAbsolutionAccount

import scala.annotation.meta.getter

@DisplayName("POST /dealer/auction/auto/strategy/delete")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
@Owner(MONEY_DUTY)
class DeleteAuctionAutoStrategyTest extends AuctionBaseTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.dealer.deleteAuctionAutoStrategy().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee401WithoutSession(): Unit = {
    val response = api.dealer
      .deleteAuctionAutoStrategy()
      .reqSpec(defaultSpec)
      .body(new RuAutoApiAuctionDeleteAuctionAutoStrategyRequest)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response)
      .hasStatus(ERROR)
      .hasError(NO_AUTH)
      .hasDetailedError("Expected dealer user. Provide valid session_id")
  }

  @Test
  def shouldSee400WithoutBody(): Unit = {
    val response = api.dealer
      .deleteAuctionAutoStrategy()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response)
      .hasStatus(ERROR)
      .hasError(BAD_REQUEST)
  }

  @Test
  def shouldSee400WithoutContextMarkCode(): Unit = {
    val sessionId = adaptor.login(getAutoAbsolutionAccount).getSession.getId
    val body = new RuAutoApiAuctionDeleteAuctionAutoStrategyRequest()
      .context(
        new RuAutoApiAuctionCallAuctionRequestContext()
          .modelCode(CHERY_BONUS_MODEL)
      )

    val response = api.dealer
      .deleteAuctionAutoStrategy()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .body(body)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response)
      .hasStatus(ERROR)
      .hasError(BAD_REQUEST)
      .hasDetailedError(s"Wrong request parameters: couldn't find info for given mark() and model($CHERY_BONUS_MODEL)")
  }

  @Test
  def shouldSee400WithoutContextModelCode(): Unit = {
    val sessionId = adaptor.login(getAutoAbsolutionAccount).getSession.getId
    val body = new RuAutoApiAuctionDeleteAuctionAutoStrategyRequest()
      .context(
        new RuAutoApiAuctionCallAuctionRequestContext()
          .markCode(CHERY_MARK)
      )

    val response = api.dealer
      .deleteAuctionAutoStrategy()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .body(body)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response)
      .hasStatus(ERROR)
      .hasError(BAD_REQUEST)
      .hasDetailedError(s"Wrong request parameters: couldn't find info for given mark($CHERY_MARK) and model()")
  }

  @Test
  def shouldSee200(): Unit = {
    val deleteBody = new RuAutoApiAuctionDeleteAuctionAutoStrategyRequest()
      .context(new RuAutoApiAuctionCallAuctionRequestContext().markCode(CHERY_MARK).modelCode(CHERY_BONUS_MODEL))

    val sessionId = adaptor.login(getAutoAbsolutionAccount).getSession.getId

    adaptor.createDealerOffer(sessionId, CARS, CHERY_OFFER_PATH)

    adaptor.createAutoStrategy(sessionId, CHERY_MARK, CHERY_BONUS_MODEL, DEFAULT_BID)

    api.dealer
      .deleteAuctionAutoStrategy()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .body(deleteBody)
      .execute(validatedWith(shouldBe200OkJSON))

    val afterDeletionState = getCurrentState(sessionId).map(state => state.getAutoStrategy)
    Assertions.assertThat(afterDeletionState.orNull).isNull()
  }
}
