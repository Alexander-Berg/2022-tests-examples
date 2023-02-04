package ru.auto.tests.publicapi.match_applications

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_REQUEST, FORBIDDEN_REQUEST, NOT_FOUND}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount

import scala.annotation.meta.getter
import scala.util.Random

@DisplayName("GET /match-applications/{match-application-id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetMatchApplicationTest {

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

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.matchApplications.getMatchApplication
      .matchApplicationIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidMatchApplicationId(): Unit = {
    val invalidMatchApplicationId = getRandomString

    val response = api.matchApplications.getMatchApplication.reqSpec(defaultSpec)
      .matchApplicationIdPath(invalidMatchApplicationId)
      .dealerIdQuery(Random.nextInt)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NOT_FOUND)
      .hasDetailedError(s"MatchApplication with id=$invalidMatchApplicationId not fund in match-maker.")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutDealerId(): Unit = {
    val response = api.matchApplications.getMatchApplication.reqSpec(defaultSpec)
      .matchApplicationIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError)
      .contains("Request is missing required query parameter 'dealer_id'")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenApplicationIdNotBelongToDealer(): Unit = {
    val dealerId = getDemoAccount.getId
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val matchApplication = adaptor.createMatchApplication(account, sessionId)

    val response = api.matchApplications.getMatchApplication
      .reqSpec(defaultSpec)
      .matchApplicationIdPath(matchApplication.getInfo.getId)
      .dealerIdQuery(dealerId)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(FORBIDDEN_REQUEST)
      .hasDetailedError(s"Match application ${matchApplication.getInfo.getId} not belong to dealer ${dealerId}")
  }
}
