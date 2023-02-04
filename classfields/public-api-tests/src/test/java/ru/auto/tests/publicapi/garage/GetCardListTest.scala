package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.{Matcher, MatcherAssert}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.model.{AutoApiVinGarageGetListingRequest, AutoApiVinGarageGetListingRequestFilters}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("POST /garage/user/cards")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCardListTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val testApi: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val am: AccountManager = null

  @Inject private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession

    val card = adaptor.createGarageCardByVinOrLp("X9FLXXEEBLES67719", session.getId)

    val filter = new AutoApiVinGarageGetListingRequest().filters(
      new AutoApiVinGarageGetListingRequestFilters()
        .addStatusItem(AutoApiVinGarageGetListingRequestFilters.StatusEnum.ACTIVE)
    )

    val getMyCards = (api: ApiClient) =>
      api
        .garage()
        .getCardsListing
        .xSessionIdHeader(session.getId)
        .reqSpec(defaultSpec())
        .body(filter)
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])

    val actual = getMyCards(testApi)
    val equalsExpected: Matcher[JsonObject] = jsonEquals(getMyCards(prodApi))

    adaptor.deleteGarageCardAndWaitRecallsDeleted(card.getCard.getId, session.getId)

    MatcherAssert.assertThat(actual, equalsExpected)
  }
}
