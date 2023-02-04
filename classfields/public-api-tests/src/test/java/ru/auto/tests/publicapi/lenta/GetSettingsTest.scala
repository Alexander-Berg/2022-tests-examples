package ru.auto.tests.publicapi.lenta

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.model.{AutoLentaParameters, AutoLentaSettings}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET lenta/get-settings")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetSettingsTest {

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

  @Test
  def getSettingWhenSettingsNotSet(): Unit = {
    api
      .lenta()
      .getSettingsLenta
      .reqSpec(defaultSpec)
      .userIdQuery("user:1234567890")
      .executeAs(validatedWith(shouldBeCode(404)))
  }

  @Test
  def getSettingsAfterSettingsCreation(): Unit = {
    val userId = "user:6"
    val settings1 =
      new AutoLentaSettings()
        .parameters(new AutoLentaParameters().sendPush(false).sendEmail(false))

    adaptor.setLentaSettings(userId, settings1)

    val settings2 =
      new AutoLentaSettings()
        .parameters(new AutoLentaParameters().sendPush(true).sendEmail(true))

    adaptor.setLentaSettings(userId, settings2)

    val settingsResponse = api
      .lenta()
      .getSettingsLenta
      .reqSpec(defaultSpec)
      .userIdQuery(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(settingsResponse.getParameters != null)
    assertThat(settingsResponse.getParameters.getSendPush)
    assertThat(settingsResponse.getParameters.getSendEmail)
  }

  @Test
  def getSettingsWithWrongUserId(): Unit = {
    api
      .lenta()
      .getSettingsLenta
      .reqSpec(defaultSpec)
      .userIdQuery("user:wrong")
      .executeAs(validatedWith(shouldBeCode(400)))
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {

    val req = (apiClient: ApiClient) =>
      apiClient
        .lenta()
        .getSettingsLenta
        .reqSpec(defaultSpec)
        .userIdQuery("user:7")
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
