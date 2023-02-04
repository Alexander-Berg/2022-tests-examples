package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.contains
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.AUTH_ERROR
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /shark/user/notification-subscriptions")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UserNotificationSubscriptionsTest {

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
    api.shark.notificationSubscriptionsUser()
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(SHARK)
  def shouldSee401WithoutSessionId(): Unit = {
    val notificationSubscriptions = api.shark.notificationSubscriptionsUser()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(notificationSubscriptions).hasError(AUTH_ERROR).hasStatus(ERROR).hasDetailedError(AUTH_ERROR.getValue)
  }

  @Test
  @Owner(SHARK)
  def shouldGetNotificationSubscriptions(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val subscriptions = api.shark.notificationSubscriptionsUser()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getSubscriptions

    MatcherAssert.assertThat(
      subscriptions,
      contains(
        new VertisSharkSubscription().channel(VertisSharkSubscription.ChannelEnum.ALL).subscribed(true),
        new VertisSharkSubscription().channel(VertisSharkSubscription.ChannelEnum.CALL).subscribed(true),
        new VertisSharkSubscription().channel(VertisSharkSubscription.ChannelEnum.PUSH).subscribed(true),
        new VertisSharkSubscription().channel(VertisSharkSubscription.ChannelEnum.SMS).subscribed(true),
        new VertisSharkSubscription().channel(VertisSharkSubscription.ChannelEnum.EMAIL).subscribed(true),
        new VertisSharkSubscription().channel(VertisSharkSubscription.ChannelEnum.MESSAGE).subscribed(true)
      )
    )
  }

  @Test
  @Owner(SHARK)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val req = (apiClient: ApiClient) => apiClient.shark.notificationSubscriptionsUser()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
