package ru.auto.tests.publicapi.offer_card

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.commons.lang3.RandomUtils
import org.apache.http.HttpStatus._
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.account.Account
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.model.AutoApiOffer
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import java.time.Duration
import scala.annotation.meta.getter

@DisplayName("GET /offer/{category}/{offerID}/reload-vin-resolution")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class ReloadVinResolutionTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val accountManager: AccountManager = null

  private val OneDayAgo = System.currentTimeMillis() - Duration.ofDays(1).toMillis;

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val offerId = adaptor.createOffer(account.getLogin, sessionId, CARS).getOfferId

    val request = (apiClient: ApiClient) =>
      apiClient.offerCard
        .reloadVinResolution()
        .reqSpec(defaultSpec)
        .categoryPath("cars")
        .offerIDPath(offerId)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)))
  }

  @Test
  @Owner(CARFAX)
  def shouldSee403WhenNoAuth(): Unit = {
    api.offerCard.offerStat
      .categoryPath(getRandomString)
      .offerIDPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(CARFAX)
  def shouldSee404WhenInvalidOfferId(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    api.offerCard.offerStat
      .reqSpec(defaultSpec)
      .categoryPath("cars")
      .offerIDPath("abcde")
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_METHOD_NOT_ALLOWED)))
  }

  @Test
  @Owner(CARFAX)
  def shouldSee200WhenValidRequest(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val offer = adaptor
      .createOffer(
        account.getLogin,
        sessionId,
        AutoApiOffer.CategoryEnum.CARS,
        "offers/offer_with_gibdd_restrictions.ftl"
      )
      .getOffer

    val request = (apiClient: ApiClient) =>
      apiClient.offerCard
        .reloadVinResolution()
        .reqSpec(defaultSpec)
        .categoryPath("cars")
        .offerIDPath(offer.getId)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBeCode(SC_OK)))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)))
  }
}
