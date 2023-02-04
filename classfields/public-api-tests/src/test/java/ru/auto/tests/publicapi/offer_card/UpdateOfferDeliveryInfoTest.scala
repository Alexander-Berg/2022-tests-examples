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
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.{PublicApiAdaptor, PublicApiDealerAdaptor}
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.model.{AutoApiDeliveryInfo, AutoApiDeliveryRegion, AutoApiGeoPoint, AutoApiLocation}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount

import scala.annotation.meta.getter

@DisplayName("PUT /offer/{category}/{offerID}/delivery")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class UpdateOfferDeliveryInfoTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val dealerAdaptor: PublicApiDealerAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.offerCard.updateDelivery()
      .categoryPath(getRandomString)
      .offerIDPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldHasNoDiffWithProduction(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession.getId
    val offer = dealerAdaptor.createDealerUsedOffer(sessionId, CARS)
    val body = new AutoApiDeliveryInfo().addDeliveryRegionsItem(new AutoApiDeliveryRegion().location(
      new AutoApiLocation().address("Leo Tolstoy, 16").coord(
        new AutoApiGeoPoint().latitude(55.94239807).longitude(37.31966782))))

    api.offerCard.updateDelivery()
      .reqSpec(defaultSpec)
      .categoryPath("cars")
      .offerIDPath(offer.getOfferId)
      .body(body)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeSuccess))
  }
}
