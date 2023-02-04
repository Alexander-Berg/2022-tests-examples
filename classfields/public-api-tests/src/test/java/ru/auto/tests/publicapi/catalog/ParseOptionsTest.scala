package ru.auto.tests.publicapi.catalog

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model.{AutoApiErrorResponse, RuAutoCatalogDescriptionParseRequest}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /reference/catalog/cars/parse-options")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class ParseOptionsTest {

  private val DESCRIPTION = new RuAutoCatalogDescriptionParseRequest()
    .description("Отличный люк в крыше, 18 диски. Стоит на сигнализации и DVD")

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.catalog.parseOptions().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit = {
    val response = api.catalog.parseOptions().reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR)
    Assertions.assertThat(response.getDetailedError)
      .contains("The request content was malformed:\nExpect message object but got: null")
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetMigrationInfoHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) => apiClient.catalog.parseOptions()
      .reqSpec(defaultSpec)
      .body(DESCRIPTION)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
