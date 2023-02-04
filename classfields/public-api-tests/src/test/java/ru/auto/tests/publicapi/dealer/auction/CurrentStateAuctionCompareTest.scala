package ru.auto.tests.publicapi.dealer.auction

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import io.qameta.allure.{Description, Owner}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{After, Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.DealerConsts.{CHERY_BONUS_MODEL, CHERY_MARK, CHERY_OFFER_PATH, DEFAULT_BID}
import ru.auto.tests.publicapi.consts.Owners.MONEY_DUTY
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getAutoAbsolutionAccount

import scala.annotation.meta.getter

@DisplayName("GET /dealer/auction/current-state")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
@Owner(MONEY_DUTY)
class CurrentStateAuctionCompareTest extends AuctionBaseTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Description("Compare json response for GET /dealer/auction/current-state")
  def shouldCurrentStateHasNoDiffWithProduction(): Unit = {
    val sessionId = adaptor.login(getAutoAbsolutionAccount).getSession.getId

    adaptor.createDealerOffer(sessionId, CARS, CHERY_OFFER_PATH)

    // Обязательно удаляем автостратегию, иначе не сможем сделать ставку
    adaptor.deleteAutoStrategy(sessionId, CHERY_MARK, CHERY_BONUS_MODEL)

    val stateBefore = getCurrentState(sessionId)
    val previousBid = stateBefore.get.getCurrentBid
    val newBid = computeNewBid(stateBefore)

    adaptor.placeBidAuction(sessionId, CHERY_MARK, CHERY_BONUS_MODEL, previousBid, newBid)

    adaptor.createAutoStrategy(sessionId, CHERY_MARK, CHERY_BONUS_MODEL, DEFAULT_BID)

    val req = (apiClient: ApiClient) =>
      apiClient.dealer.currentState
        .reqSpec(defaultSpec)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }

  @After
  @Description("Удаляем за собой автостратегию, иначе могут падать другие тесты")
  def deleteAutoStrategy(): Unit = {
    val sessionId = adaptor.login(getAutoAbsolutionAccount).getSession.getId
    adaptor.deleteAutoStrategy(sessionId, CHERY_MARK, CHERY_BONUS_MODEL)
  }
}
