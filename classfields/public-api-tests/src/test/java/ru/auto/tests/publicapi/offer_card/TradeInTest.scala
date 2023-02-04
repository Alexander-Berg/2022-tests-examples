package ru.auto.tests.publicapi.offer_card

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.{PublicApiAdaptor, PublicApiDealerAdaptor}
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.model.{AutoCabinetTradeInRequestForm, AutoCabinetTradeInRequestFormClientInfo, AutoCabinetTradeInRequestFormOfferDescription, AutoCabinetTradeInRequestFormOfferInfo, AutoCabinetTradeInRequestFormUserInfo}
import ru.auto.tests.publicapi.model.AutoCabinetTradeInRequestFormOfferInfo.CategoryEnum.{CARS => CABINET_CARS}
import ru.auto.tests.publicapi.model.AutoCabinetTradeInRequestFormOfferInfo.SectionEnum.{USED, NEW}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount

import scala.annotation.meta.getter

@DisplayName("POST /offer/{category}/{offerID}/trade-in")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class TradeInTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val dealerAdaptor: PublicApiDealerAdaptor = null

  @Inject
  private val accountManager: AccountManager = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.offerCard.tradeIn()
      .categoryPath(getRandomString)
      .offerIDPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val userSessionId = adaptor.login(account).getSession.getId
    val dealerSessionId = adaptor.login(getDemoAccount).getSession.getId
    val userOffer = adaptor.createOffer(account.getLogin, userSessionId, CARS)
    val dealerOffer = dealerAdaptor.createDealerOffer(dealerSessionId, CARS, "offers/dealer_new_cars.json")

    val body = new AutoCabinetTradeInRequestForm()
      .userInfo(new AutoCabinetTradeInRequestFormUserInfo().phoneNumber(account.getLogin).name("Иван Иванов"))
      .clientInfo(new AutoCabinetTradeInRequestFormClientInfo().clientId(20101L))
      .clientOfferInfo(new AutoCabinetTradeInRequestFormOfferInfo().offerId(dealerOffer.getOfferId)
        .category(CABINET_CARS).section(NEW)
        .description(new AutoCabinetTradeInRequestFormOfferDescription()))
      .userOfferInfo(new AutoCabinetTradeInRequestFormOfferInfo().offerId(userOffer.getOfferId)
        .category(CABINET_CARS).section(USED)
        .description(new AutoCabinetTradeInRequestFormOfferDescription()))

    api.offerCard.tradeIn()
      .reqSpec(defaultSpec)
      .categoryPath("cars")
      .offerIDPath(dealerOffer.getOfferId)
      .body(body)
      .xSessionIdHeader(userSessionId)
      .execute(validatedWith(shouldBeSuccess))
  }
}
