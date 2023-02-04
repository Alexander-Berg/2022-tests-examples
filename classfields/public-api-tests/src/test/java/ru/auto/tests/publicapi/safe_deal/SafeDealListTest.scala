package ru.auto.tests.publicapi.safe_deal

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.{Description, Owner, Step}
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.CoreMatchers.{anyOf, equalTo, hasItem, hasItems}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Before, Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.SAFE_DEAL
import ru.auto.tests.publicapi.model._

import scala.jdk.CollectionConverters._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.model.VertisSafeDealDealView.{ParticipantTypeEnum, StepEnum}
import ru.auto.tests.publicapi.safe_deal.SafeDealFlowTest.{AllowedForSafeDealTag, setOfferTag, waitUntil}
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.config.PublicApiConfig

import scala.annotation.meta.getter
import java.util

@DisplayName("GET /safe-deal/deal/list")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class SafeDealListTest {

  import SafeDealListTest._

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

  private var buyerSessionId: String = _

  private var offer1Id: String = _
  private var offer2Id: String = _

  private var deal1Id: String = _
  private var deal2Id: String = _

  @Before
  @Description("Создаём пользовательскую сессию, офферы и сделки")
  def prepare(): Unit = {
    val buyerAccount = accountManager.create()
    buyerSessionId = adaptor.login(buyerAccount).getSession.getId
    val seller1Account = accountManager.create()
    val seller1SessionId = adaptor.login(seller1Account).getSession.getId
    offer1Id = adaptor.createOffer(seller1Account.getLogin, seller1SessionId, CARS).getOfferId
    val seller2Account = accountManager.create()
    val seller2SessionId = adaptor.login(seller2Account).getSession.getId
    offer2Id = adaptor.createOffer(seller2Account.getLogin, seller2SessionId, CARS).getOfferId

    waitUntil(
      () => adaptor.getOfferWithSession(CARS, offer1Id, seller1SessionId).getOffer.getStatus,
      equalTo(AutoApiOffer.StatusEnum.ACTIVE)
    )

    setOfferTag(config.getVosApiURI, seller1Account.getId, offer1Id, AllowedForSafeDealTag)

    deal1Id = adaptor.createDealByBuyer(buyerSessionId, offer1Id).getId

    waitUntil(
      () => adaptor.getOfferWithSession(CARS, offer2Id, seller2SessionId).getOffer.getStatus,
      equalTo(AutoApiOffer.StatusEnum.ACTIVE)
    )

    setOfferTag(config.getVosApiURI, seller2Account.getId, offer2Id, AllowedForSafeDealTag)

    deal2Id = {
      val dealId = adaptor.createDealByBuyer(buyerSessionId, offer2Id).getId
      adaptor.acceptDealBySeller(seller2SessionId, dealId, true).getId
    }
  }

  @Test
  @Owner(SAFE_DEAL)
  @Description("Проверяем, что получаем правильный список сделок по пользователю")
  def shouldGetDealList(): Unit = {
    val dealIds = api.dealList(buyerSessionId).map(_.getId)
    MatcherAssert.assertThat(dealIds, hasItems(deal1Id, deal2Id))
  }

  @Test
  @Owner(SAFE_DEAL)
  @Description("Проверяем, что получаем правильный список сделок по пользователю, где он является покупателем")
  def shouldGetDealListByParty(): Unit = {
    val dealIdsByParty = api.dealList(buyerSessionId, ParticipantTypeEnum.BUYER).map(_.getId)
    MatcherAssert.assertThat(dealIdsByParty, hasItems(deal1Id, deal2Id))
  }

  @Test
  @Owner(SAFE_DEAL)
  @Description("Проверяем, что получаем правильный список сделок по пользователю и id оффера")
  def shouldGetDealListByOfferId(): Unit = {
    val dealIdsByOfferId = api.dealList(buyerSessionId, offer1Id).map(_.getId)
    MatcherAssert.assertThat(dealIdsByOfferId, equalTo(Seq(deal1Id).asJava))
  }

  @Test
  @Owner(SAFE_DEAL)
  @Description("Проверяем, что получаем правильный список сделок по пользователю, отфильтрованному по шагу сделки")
  def shouldGetDealListByStep(): Unit = {
    val dealIdsByStep = api.dealList(buyerSessionId, StepEnum.INVITE_ACCEPTED).map(_.getId)
    MatcherAssert.assertThat(dealIdsByStep, equalTo(Seq(deal2Id).asJava))
  }

  @Test
  @Owner(SAFE_DEAL)
  @Description("Проверяем, что получаем правильный список сделок по пользователю с пагинацией")
  def shouldGetDealListWithPagination(): Unit = {
    val dealsWithPagination = api.dealList(buyerSessionId, 1, 1).map(_.getId)
    MatcherAssert.assertThat(dealsWithPagination, anyOf(hasItem(deal1Id), hasItem(deal2Id)))
  }
}

object SafeDealListTest {

  implicit private class RichApiClient(val api: ApiClient) extends AnyVal {

    private def dealList(sessionId: String,
                         partyOpt: Option[ParticipantTypeEnum] = None,
                         offerIdOpt: Option[String] = None,
                         stepOpt: Option[StepEnum] = None,
                         pageOpt: Option[Int] = None,
                         pageSizeOpt: Option[Int] = None) = {
      val request = api.safeDeal()
        .dealList()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(sessionId)

      partyOpt.foreach(party => request.partyQuery(party.name.toLowerCase))
      offerIdOpt.foreach(offerId => request.offerIdsQuery(offerId))
      stepOpt.foreach(step => request.stepsQuery(step))
      pageOpt.foreach(page => pageSizeOpt.foreach(pageSize => request.pageQuery(page).pageSizeQuery(pageSize)))

      request
        .executeAs(validatedWith(shouldBe200OkJSON))
        .getDeals
    }

    @Step("Получаем список сделок для сессии {sessionId}")
    def dealList(sessionId: String): util.List[VertisSafeDealDealView] =
      dealList(sessionId = sessionId, partyOpt = None, offerIdOpt = None, stepOpt = None, pageOpt = None, pageSizeOpt = None)

    @Step("Получаем список сделок для сессии {sessionId} и типа участника {party}")
    def dealList(sessionId: String, party: ParticipantTypeEnum): util.List[VertisSafeDealDealView] =
      dealList(sessionId = sessionId, partyOpt = Some(party), offerIdOpt = None, stepOpt = None, pageOpt = None, pageSizeOpt = None)

    @Step("Получаем список сделок для сессии {sessionId} и оффера {offerId}")
    def dealList(sessionId: String, offerId: String): util.List[VertisSafeDealDealView] =
      dealList(sessionId = sessionId, partyOpt = None, offerIdOpt = Some(offerId), stepOpt = None, pageOpt = None, pageSizeOpt = None)

    @Step("Получаем список сделок для сессии {sessionId} и шага сделки {step}")
    def dealList(sessionId: String, step: StepEnum): util.List[VertisSafeDealDealView] =
      dealList(sessionId = sessionId, partyOpt = None, offerIdOpt = None, stepOpt = Some(step), pageOpt = None, pageSizeOpt = None)

    @Step("Получаем список сделок для сессии {sessionId} с пагинацией page = {page}, pageSize = {pageSize}")
    def dealList(sessionId: String, page: Int, pageSize: Int): util.List[VertisSafeDealDealView] =
      dealList(sessionId = sessionId, partyOpt = None, offerIdOpt = None, stepOpt = None, pageOpt = Some(page), pageSizeOpt = Some(pageSize))
  }

  implicit private class RichVertisSafeDealDealViewList(val value: util.List[VertisSafeDealDealView]) extends AnyVal {

    def map[T](f: VertisSafeDealDealView => T): util.List[T] = value.asScala.map(f).asJava
  }
}
