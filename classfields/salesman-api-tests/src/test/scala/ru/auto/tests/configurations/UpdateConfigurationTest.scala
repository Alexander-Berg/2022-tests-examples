package ru.auto.tests.configurations

import java.util.stream.StreamSupport

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonArray
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.assertj.core.api.Assertions.assertThat
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.{
  shouldBe200WithMessageOK,
  shouldBe400WithMissedSalesmanUser
}

import scala.annotation.meta.getter

@DisplayName("PUT /service/{service}/configurations/{name}")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[SalesmanApiModule]))
class UpdateConfigurationTest {

  private val CONFIGURATION_NAME = "qa-auto-test-text"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSeeNewConfigurationValueAfterUpdate(): Unit = {
    val configurationValue = getRandomString

    api.configurations
      .updateConfigEntry()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .namePath(CONFIGURATION_NAME)
      .body(configurationValue)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200WithMessageOK))

    val response = api.configurations.getConfigs
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonArray])

    val feature = StreamSupport
      .stream(response.spliterator, false)
      .filter { jsonElement =>
        jsonElement.getAsJsonObject
          .get("name")
          .getAsString == CONFIGURATION_NAME
      }
      .findFirst
      .get
      .getAsJsonObject

    assertThat(feature.get("value").getAsString).isEqualTo(configurationValue)
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidService(): Unit =
    api.configurations
      .updateConfigEntry()
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .namePath(CONFIGURATION_NAME)
      .body(getRandomString)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidFeatureName(): Unit =
    api.configurations
      .updateConfigEntry()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .namePath(getRandomString)
      .body(getRandomString)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithMissedSalesmanUserHeader(): Unit =
    api.configurations
      .updateConfigEntry()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .namePath(CONFIGURATION_NAME)
      .body(getRandomString)
      .execute(validatedWith(shouldBe400WithMissedSalesmanUser))
}
