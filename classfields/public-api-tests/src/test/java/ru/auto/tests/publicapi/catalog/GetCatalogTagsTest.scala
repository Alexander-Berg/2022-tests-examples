package ru.auto.tests.publicapi.catalog

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.DICTIONARY_FORMAT_NOT_FOUND
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /reference/catalog/tags/{format}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCatalogTagsTest {

  private val FORMAT = "v1"

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
    api.catalog.tags.formatPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidFormat(): Unit = {
    val response = api.catalog.tags.reqSpec(defaultSpec)
      .formatPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(DICTIONARY_FORMAT_NOT_FOUND).hasStatus(ERROR)
      .hasDetailedError(DICTIONARY_FORMAT_NOT_FOUND.getValue)
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetMigrationInfoHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) => apiClient.catalog.tags
      .reqSpec(defaultSpec)
      .formatPath(FORMAT)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
