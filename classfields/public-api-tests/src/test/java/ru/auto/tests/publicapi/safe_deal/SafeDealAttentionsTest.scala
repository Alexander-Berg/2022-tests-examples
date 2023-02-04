package ru.auto.tests.publicapi.safe_deal

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import io.qameta.allure.{Owner, Step}
import org.hamcrest.CoreMatchers.{equalTo, not, nullValue}
import org.hamcrest.Matchers.empty
import org.hamcrest.MatcherAssert
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.passport.account.Account
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.config.PublicApiConfig
import ru.auto.tests.publicapi.consts.Owners.SAFE_DEAL
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.model.{VertisSafeDealApiUserAttentionsResponseAttention => Attention}
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.safe_deal.SafeDealFlowTest._

import java.time.Instant
import java.util
import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("GET /safe-deal/attention/unread")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class SafeDealAttentionsTest {

  import SafeDealAttentionsTest._

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val config: PublicApiConfig = null

  @Test
  @Owner(SAFE_DEAL)
  def shouldBeEmptyAttentionSeq(): Unit = {
    val buyerAccount = accountManager.create()
    val buyerSessionId = adaptor.login(buyerAccount).getSession.getId
    val sellerAccount = accountManager.create()
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    val offerId = createOffer(sellerAccount, sellerSessionId)

    adaptor.createDealByBuyer(buyerSessionId, offerId)

    checkReadAttentions(buyerSessionId)
  }

  @Test
  @Owner(SAFE_DEAL)
  def shouldBeSellerAcceptingDealTemplate(): Unit = {
    val buyerAccount = accountManager.create()
    val buyerSessionId = adaptor.login(buyerAccount).getSession.getId
    val sellerAccount = accountManager.create()
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    val offerId = createOffer(sellerAccount, sellerSessionId)

    adaptor.createDealByBuyer(buyerSessionId, offerId)

    checkUnreadAttentions(sellerSessionId, SellerAcceptingDealTemplate)
  }

  @Test
  @Owner(SAFE_DEAL)
  def shouldBeOneRequestWithActiveDealTemplate(): Unit = {
    val buyerAccount = accountManager.create()
    val buyerSessionId = adaptor.login(buyerAccount).getSession.getId
    val sellerAccount = accountManager.create()
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    val offerId = createOffer(sellerAccount, sellerSessionId, withActiveSafeDealExistsTag = true)

    adaptor.createDealByBuyer(buyerSessionId, offerId)

    checkUnreadAttentions(sellerSessionId, OneRequestWithActiveDealTemplate)
  }

  @Test
  @Owner(SAFE_DEAL)
  def shouldBeManyRequestsForOneOfferTemplate(): Unit = {
    val buyer1Account = accountManager.create()
    val buyer1SessionId = adaptor.login(buyer1Account).getSession.getId
    val buyer2Account = accountManager.create()
    val buyer2SessionId = adaptor.login(buyer2Account).getSession.getId
    val sellerAccount = accountManager.create()
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    val offerId = createOffer(sellerAccount, sellerSessionId)

    adaptor.createDealByBuyer(buyer1SessionId, offerId)
    adaptor.createDealByBuyer(buyer2SessionId, offerId)

    checkUnreadAttentions(sellerSessionId, ManyRequestsForOneOfferTemplate)
  }

  @Test
  @Owner(SAFE_DEAL)
  def shouldBeManyRequestsForDifferentOffersTemplate(): Unit = {
    val buyerAccount = accountManager.create()
    val buyerSessionId = adaptor.login(buyerAccount).getSession.getId
    val sellerAccount = accountManager.create()
    val sellerSessionId = adaptor.login(sellerAccount).getSession.getId
    val offer1Id = createOffer(sellerAccount, sellerSessionId)
    val offer2Id = createOffer(sellerAccount, sellerSessionId)

    adaptor.createDealByBuyer(buyerSessionId, offer1Id)
    adaptor.createDealByBuyer(buyerSessionId, offer2Id)

    checkUnreadAttentions(sellerSessionId, ManyRequestsForDifferentOffersTemplate)
  }

  private def createOffer(account: Account, sessionId: String, withActiveSafeDealExistsTag: Boolean = false): String = {
    val offerId = adaptor.createOffer(account.getLogin, sessionId, CARS).getOfferId
    val activeTag = if (withActiveSafeDealExistsTag) Some(ActiveSafeDealExistsTag) else None
    val tags = Some(AllowedForSafeDealTag) ++ activeTag
    setOfferTag(config.getVosApiURI, account.getId, offerId, tags.mkString(","))
    waitUntil(
      () => adaptor.getOfferWithSession(CARS, offerId, sessionId).getOffer.getStatus,
      equalTo(AutoApiOffer.StatusEnum.ACTIVE)
    )
    offerId
  }

  private def checkUnreadAttentions(sessionId: String, expected: String): Unit = {
    waitUntil(() => api.unreadAttentions(sessionId), not(empty[Attention]))
    val template = api.unreadAttentions(sessionId).asScala.head.getTemplate
    MatcherAssert.assertThat(template, equalTo(expected))
  }

  private def checkReadAttentions(sessionId: String): Unit = {
    waitUntil(() => api.unreadAttentions(sessionId), not(empty[Attention]))
    api.readAttentions(sessionId)
    val attentions = api.unreadAttentions(sessionId)
    MatcherAssert.assertThat(attentions, nullValue)
  }
}

object SafeDealAttentionsTest {

  private val ActiveSafeDealExistsTag = "active_safe_deal_exists"
  private val SellerAcceptingDealTemplate = "seller-accepting-deal-seller"
  private val OneRequestWithActiveDealTemplate = s"$SellerAcceptingDealTemplate|one-request-with-active-deal"
  private val ManyRequestsForOneOfferTemplate = s"$SellerAcceptingDealTemplate|many-requests-for-one-offer"
  private val ManyRequestsForDifferentOffersTemplate = s"$SellerAcceptingDealTemplate|many-requests-for-different-offers"

  implicit class RichApiClient(val value: ApiClient) extends AnyVal {


    @Step("Получаем непрочитанные уведомления для сессии {sessionId}")
    def unreadAttentions(sessionId: String): util.List[Attention] =
      value.safeDeal()
        .unreadUserAttentions()
        .reqSpec(defaultSpec)
        .withOffersQuery(true)
        .xSessionIdHeader(sessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))
        .getAttentions

    @Step("Помечаем уведомления как прочитанные для сессии {sessionId}")
    def readAttentions(sessionId: String): Unit =
      value.safeDeal()
        .readUserAttentions()
        .reqSpec(defaultSpec)
        .updatedQuery(Instant.now.toString)
        .xSessionIdHeader(sessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))
  }
}
