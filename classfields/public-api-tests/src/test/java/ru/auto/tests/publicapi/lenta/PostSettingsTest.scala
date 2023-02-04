package ru.auto.tests.publicapi.lenta

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.model.{AutoLentaParameters, AutoLentaSettings}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST lenta/set-settings")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PostSettingsTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {

    val userId = "user:11"

    val settings =
      new AutoLentaSettings()
        .parameters(new AutoLentaParameters().sendPush(true).sendEmail(true))

    val req = (apiClient: ApiClient) =>
      apiClient
      .lenta()
      .setSettingsLenta()
      .body(settings)
      .reqSpec(defaultSpec)
      .userIdQuery(userId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    val settings =
      new AutoLentaSettings()
        .parameters(new AutoLentaParameters().sendPush(true).sendEmail(true))
    api.lenta().setSettingsLenta()
      .body(settings)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400WithoutBody(): Unit = {
    api.lenta().setSettingsLenta()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def shouldSee400OnInvalidUserId(): Unit = {
    val userId = "invalid_user:123"
    val settings =
      new AutoLentaSettings()
        .parameters(new AutoLentaParameters().sendPush(true).sendEmail(true))
    api.lenta().setSettingsLenta()
      .reqSpec(defaultSpec)
      .body(settings)
      .userIdQuery(userId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }
}
