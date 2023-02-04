package ru.auto.tests.publicapi.dealer

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_OK, SC_UNAUTHORIZED}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.assertj.core.api.Assertions
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH
import ru.auto.tests.publicapi.model.{AutoDealerStatsFastSellIndicator, AutoDealerStatsRevaluationIndicator, AutoDealerStatsToxicStockIndicator}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getTestAccount
import ru.auto.tests.utils.AssertUtils.assertApiError

import scala.jdk.CollectionConverters._
import scala.annotation.meta.getter
import scala.util.Try

@DisplayName("GET /dealer/warehouse/widget")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetWarehouseWidgetTest {

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

  private def validateToxicStock(toxicStock: AutoDealerStatsToxicStockIndicator) = {
    val validToxicStockIndicatorColors = List("GREEN", "YELLOW", "RED")

    Assertions.assertThat(toxicStock).hasFieldOrProperty("color")
    Assertions.assertThat(toxicStock).hasFieldOrProperty("percent")
    Assertions.assertThat(toxicStock).hasFieldOrProperty("modifiedAt")
    Assertions.assertThat(toxicStock.getColor.getValue).isIn(validToxicStockIndicatorColors.asJava)
    Assertions.assertThat(toxicStock.getPercent).isBetween(0, 100)
  }

  private def validateRevaluation(revaluation: AutoDealerStatsRevaluationIndicator) = {
    val validRevaluationIndicatorColors = List("GREEN", "YELLOW", "ORANGE", "RED")

    Assertions.assertThat(revaluation).hasFieldOrProperty("color")
    Assertions.assertThat(revaluation).hasFieldOrProperty("score")
    Assertions.assertThat(revaluation).hasFieldOrProperty("modifiedAt")
    Assertions.assertThat(revaluation.getColor.getValue).isIn(validRevaluationIndicatorColors.asJava)
    MatcherAssert
      .assertThat(
        s"Percent of revaluation must be positive integer, but ${revaluation.getScore}",
        Try(revaluation.getScore.toInt >= 0).getOrElse(false)
      )
  }

  private def validateFastSell(fastSell: AutoDealerStatsFastSellIndicator) = {
    val validFastSellIndicatorColors = List("GREEN", "YELLOW", "RED")

    Assertions.assertThat(fastSell).hasFieldOrProperty("color")
    Assertions.assertThat(fastSell).hasFieldOrProperty("percent")
    Assertions.assertThat(fastSell).hasFieldOrProperty("modifiedAt")
    Assertions.assertThat(fastSell.getColor.getValue).isIn(validFastSellIndicatorColors.asJava)
    Assertions.assertThat(fastSell.getPercent).isBetween(0, 100)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee403WhenNoAuth(): Unit = {
    api.dealer.getWarehouseWidget.execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee401WhenAnonym(): Unit = {
    val sessionId = adaptor.session().getSession.getId()
    assertApiError(NO_AUTH) {
      api.dealer.getWarehouseWidget
        .reqSpec(defaultSpec)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSee401WithoutSessionId(): Unit = {
    assertApiError(NO_AUTH, detailedError = Some("Expected dealer user. Provide valid session_id")) {
      api.dealer.getWarehouseWidget
        .reqSpec(defaultSpec)
        .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
    }
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSuccessValidateResponse(): Unit = {
    val sessionId = adaptor.login(getTestAccount).getSession.getId

    val result = api.dealer.getWarehouseWidget
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBeCode(SC_OK)))

    Assertions.assertThat(result).hasFieldOrProperty("toxicStock")
    validateToxicStock(result.getToxicStock)
    Assertions.assertThat(result).hasFieldOrProperty("revaluation")
    validateRevaluation(result.getRevaluation)
    Assertions.assertThat(result).hasFieldOrProperty("fastSell")
    validateFastSell(result.getFastSell)
  }

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldHasNoDiffWithProduction(): Unit = {
    val sessionId = adaptor.login(getTestAccount).getSession.getId

    val req = (apiClient: ApiClient) =>
      apiClient.dealer.getWarehouseWidget
        .reqSpec(defaultSpec)
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }

}
