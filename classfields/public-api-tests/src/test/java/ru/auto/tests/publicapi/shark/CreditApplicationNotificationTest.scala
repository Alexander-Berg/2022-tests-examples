package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
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

@DisplayName("PUT /shark/credit-application/notification/{credit_application_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreditApplicationNotificationTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(SHARK)
  def shouldSee403WhenNoAuth(): Unit = {
    api.shark.addNotification()
      .creditApplicationIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(SHARK)
  def shouldSee401WithoutSessionId(): Unit = {
    val response = api.shark.addNotification()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(AUTH_ERROR).hasStatus(ERROR).hasDetailedError(AUTH_ERROR.getValue)
  }

  @Test
  @Owner(SHARK)
  def shouldAddNotification(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val creditApplicationId = adaptor.createCreditApplication(sessionId).getCreditApplication.getId

    api.shark.addNotification()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(creditApplicationId)
      .body(
        new VertisSharkNotificationSource()
          .notificationType(VertisSharkNotificationSource.NotificationTypeEnum.CHAT)
          .messagePayload(
            new VertisSharkNotificationMessagePayload()
              .chat(
                new VertisSharkNotificationMessagePayloadChat()
                  .payload(
                    new VertisSharkNotificationMessagePayloadChatPayload()
                      .body("message")
                  )
              )
          )
      )
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }
}
