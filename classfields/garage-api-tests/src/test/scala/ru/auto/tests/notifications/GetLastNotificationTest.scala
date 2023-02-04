package ru.auto.tests.notifications

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND, SC_OK}
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.Base
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.constants.Owners.CARFAX
import ru.auto.tests.garage.ApiClient
import ru.auto.tests.module.GarageApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /user/notifications/last")
@GuiceModules(Array(classOf[GarageApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetLastNotificationTest extends Base {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(CARFAX)
  def shouldSee400ForIncorrectUser(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient
        .user()
        .getLastNotification
        .platformQuery("web")
        .xUserIdHeader("user")
        .reqSpec(defaultSpec)
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi))
    )
  }

  @Test
  @Owner(CARFAX)
  def shouldSee200WithEmptyResponse(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient
        .user()
        .getLastNotification
        .xUserIdHeader("user:123")
        .platformQuery("web")
        .reqSpec(defaultSpec)
        .execute(validatedWith(shouldBeCode(SC_OK)))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi))
    )
  }
}
