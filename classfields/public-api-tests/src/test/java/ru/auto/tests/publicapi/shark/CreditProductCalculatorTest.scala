package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.junit.{Ignore, Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, validatedWith}
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.model.{VertisSharkApiCreditProductsRequest, VertisSharkApiCreditProductsRequestByGeo}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.SHARK

@DisplayName("GET /shark/credit-product/calculator")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreditProductCalculatorTest {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(SHARK)
  @Ignore // @see AUTORUBACK-2846
  def shouldGetCreditProductsCalculation(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val creditProducts = api.shark().creditProductList()
      .body(new VertisSharkApiCreditProductsRequest().byGeo(
        new VertisSharkApiCreditProductsRequestByGeo()
          .geobaseIds(List(Long.box(TestProductGeoId)).asJava)
      ))
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON()))
      .getCreditProducts.asScala

    val amountRangeFrom = creditProducts.map(_.getAmountRange.getFrom).min
    val amountRangeTo = creditProducts.map(_.getAmountRange.getTo).max
    val interestRangeFrom = creditProducts.map(_.getInterestRateRange.getFrom).min
    val interestRangeTo = creditProducts.map(_.getInterestRateRange.getTo).max
    val termMonthsRangeFrom = creditProducts.map(_.getTermMonthsRange.getFrom).min
    val termMonthsRangeTo = creditProducts.map(_.getTermMonthsRange.getTo).max
    val minInitialFeeRate = creditProducts.map(_.getMinInitialFeeRate).min

    val resp = api.shark().creditProductCalculator()
      .body(new VertisSharkApiCreditProductsRequest().byGeo(
        new VertisSharkApiCreditProductsRequestByGeo()
          .geobaseIds(List(Long.box(TestProductGeoId)).asJava)
      ))
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON()))

    assertThat(resp.getAmountRange.getFrom).isEqualTo(amountRangeFrom)
    assertThat(resp.getAmountRange.getTo).isEqualTo(amountRangeTo)
    assertThat(resp.getInterestRateRange.getFrom).isEqualTo(interestRangeFrom)
    assertThat(resp.getInterestRateRange.getTo).isEqualTo(interestRangeTo)
    assertThat(resp.getTermMonthsRange.getFrom).isEqualTo(termMonthsRangeFrom)
    assertThat(resp.getTermMonthsRange.getTo).isEqualTo(termMonthsRangeTo)
    assertThat(resp.getMinInitialFeeRate).isEqualTo(minInitialFeeRate)
  }

  @Test
  @Owner(SHARK)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val req = (apiClient: ApiClient) => apiClient.shark.creditProductCalculator()
      .body(new VertisSharkApiCreditProductsRequest().byGeo(
        new VertisSharkApiCreditProductsRequestByGeo()
          .geobaseIds(List(Long.box(TestProductGeoId)).asJava)
      ))
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
