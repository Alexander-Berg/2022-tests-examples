package ru.auto.tests.publicapi.app2app

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_UNAUTHORIZED}
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode}
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.PrivateAccounts

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("GET /user/app2app/call-info/")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class App2AppCallInfoTest {
  import App2AppCallInfoTest._

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val accountManager: AccountManager = null

  @Test
  def shouldSuccessGetCallInfo(): Unit = {
    val sellerAccount = PrivateAccounts.getAccountWithRedirectedPhone
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    adaptor.voxSignUpUser(sellerSessionId, true)
    val offerId = adaptor.createOffer(sellerAccount.getLogin, sellerSessionId, CARS, OfferTemplatePath).getOfferId

    val buyerAccount = accountManager.create()
    val buyerSessionId = adaptor.login(buyerAccount).getSession.getId
    adaptor.voxSignUpUser(buyerSessionId, true)

    val phones = api.offerCard.getOfferPhones.reqSpec(defaultSpec)
      .xSessionIdHeader(buyerSessionId)
      .categoryPath(CARS)
      .offerIDPath(offerId)
      .executeAs(validatedWith(shouldBe200OkJSON))
    Assertions.assertThat(phones.getApp2appHandle).isNotEmpty()

    val result = api.userApp2app().getCallInfo().reqSpec(defaultSpec)
      .xSessionIdHeader(buyerSessionId)
      .handleQuery(phones.getApp2appHandle)
      .executeAs(validatedWith(shouldBe200OkJSON))
    Assertions.assertThat(result.getApp2appVideoCallAvailable).isEqualTo(true)
    Assertions.assertThat(result.getApp2appCallAvailable).isEqualTo(true)
    Assertions.assertThat(result.getCalleeAlias).isEqualTo(sellerAccount.getId)
    Assertions.assertThat(result.getFallbackPhone).isEqualTo(phones.getPhones.asScala.head.getPhone)
  }

  @Test
  def shouldBeNotAvailableWithSignUpWithoutFeature(): Unit = {
    val sellerAccount = PrivateAccounts.getAccountWithRedirectedPhone
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    adaptor.voxSignUpUser(sellerSessionId, true)
    val offerId = adaptor.createOffer(sellerAccount.getLogin, sellerSessionId, CARS, OfferTemplatePath).getOfferId

    val buyerAccount = accountManager.create()
    val buyerSessionId = adaptor.login(buyerAccount).getSession.getId
    adaptor.voxSignUpUser(buyerSessionId, false)

    val phones = api.offerCard.getOfferPhones.reqSpec(defaultSpec)
      .xSessionIdHeader(buyerSessionId)
      .categoryPath(CARS)
      .offerIDPath(offerId)
      .executeAs(validatedWith(shouldBe200OkJSON))
    Assertions.assertThat(phones.getApp2appHandle).isNotEmpty()

    val result = api.userApp2app().getCallInfo().reqSpec(defaultSpec)
      .xSessionIdHeader(buyerSessionId)
      .handleQuery(phones.getApp2appHandle)
      .executeAs(validatedWith(shouldBe200OkJSON))
    Assertions.assertThat(result.getApp2appCallAvailable).isEqualTo(false)
  }

  @Test
  def shouldBeNotAvailableForAnonymousUser(): Unit = {
    val sellerAccount = PrivateAccounts.getAccountWithRedirectedPhone
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    adaptor.voxSignUpUser(sellerSessionId, true)
    val offerId = adaptor.createOffer(sellerAccount.getLogin, sellerSessionId, CARS, OfferTemplatePath).getOfferId

    val phones = api.offerCard.getOfferPhones.reqSpec(defaultSpec)
      .xSessionIdHeader(sellerSessionId)
      .categoryPath(CARS)
      .offerIDPath(offerId)
      .executeAs(validatedWith(shouldBe200OkJSON))
    Assertions.assertThat(phones.getApp2appHandle).isNotEmpty()

    val result = api.userApp2app().getCallInfo().reqSpec(defaultSpec)
      .handleQuery(phones.getApp2appHandle)
      .executeAs(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
    Assertions.assertThat(result.getApp2appCallAvailable).isNull()
  }

  @Test
  def shouldBeEmptyHandleForAnonymousUser(): Unit = {
    val sellerAccount = PrivateAccounts.getAccountWithRedirectedPhone
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    adaptor.voxSignUpUser(sellerSessionId, true)
    val offerId = adaptor.createOffer(sellerAccount.getLogin, sellerSessionId, CARS, OfferTemplatePath).getOfferId

    val phones = api.offerCard.getOfferPhones.reqSpec(defaultSpec)
      .categoryPath(CARS)
      .offerIDPath(offerId)
      .executeAs(validatedWith(shouldBe200OkJSON))
    Assertions.assertThat(phones.getApp2appHandle).isNull()
  }

  @Test
  def shouldBeErrorWithBrokenHandle(): Unit = {
    val buyerAccount = accountManager.create()
    val buyerSessionId = adaptor.login(buyerAccount).getSession.getId
    adaptor.voxSignUpUser(buyerSessionId, true)

    api.userApp2app().getCallInfo().reqSpec(defaultSpec)
      .xSessionIdHeader(buyerSessionId)
      .handleQuery("abcdefg")
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }
}

object App2AppCallInfoTest {
  val OfferTemplatePath: String = "offers/cars_with_redirect_phone.ftl"
}
