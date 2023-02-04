package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.AUTH_ERROR
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model.VertisSharkCreditApplicationSource.{StateEnum => SourceStateEnum}
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /shark/credit-application/active")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class ActiveCreditApplicationTest {

  @(Rule @getter)
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
  def shouldSee403WhenNoAuth(): Unit = {
    api.shark.creditApplicationActive
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(SHARK)
  def shouldSee401WithoutSessionId(): Unit = {
    val response = api.shark.creditApplicationActive.reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(AUTH_ERROR).hasStatus(ERROR)
      .hasDetailedError(AUTH_ERROR.getValue)
  }

  @Test
  @Owner(SHARK)
  def shouldGetActive(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val firstCreditApplication = adaptor.createCreditApplication(sessionId)
    val responseAfterFirst = api.shark.creditApplicationActive()
        .reqSpec(defaultSpec)
        .xSessionIdHeader(sessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))
    assertThat(responseAfterFirst.getCreditApplication).hasId(firstCreditApplication.getCreditApplication.getId)

    api.shark.creditApplicationUpdate()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(firstCreditApplication.getCreditApplication.getId)
      .body(new VertisSharkCreditApplicationSource().state(SourceStateEnum.CANCELED))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val responseAfterUpdate = api.shark.creditApplicationActive()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))
    assertThat(responseAfterUpdate.getCreditApplication).isNull

    val secondCreditApplication = adaptor.createCreditApplication(sessionId)
    val responseAfterSecond = api.shark.creditApplicationActive()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))
    assertThat(responseAfterSecond.getCreditApplication).hasId(secondCreditApplication.getCreditApplication.getId)
  }

  @Test
  @Owner(SHARK)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val creditApplication = adaptor.createCreditApplication(sessionId)

    val req = (apiClient: ApiClient) => apiClient.shark.creditApplicationActive()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
