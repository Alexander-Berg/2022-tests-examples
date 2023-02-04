package ru.auto.tests.publicapi.panorama

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.github.benas.randombeans.api.EnhancedRandom.random
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
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.{AutoApiErrorResponse, AutoPanoramasMetadata, VertisRequestContextDeviceInfo}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_REQUEST, NOT_FOUND}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("GET /panorama/{panorama_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UpdatePanoramaMetaDataTest {

  private val PANORAMA_ID = "1695162630-1603273781333-zDOOH"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.exteriorPanorama.addMetadataExt()
      .panoramaIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidPanoramaId(): Unit = {
    val invalidPanoramaId = getRandomString

    val response = api.exteriorPanorama.addMetadataExt().reqSpec(defaultSpec)
      .panoramaIdPath(invalidPanoramaId)
      .body(new AutoPanoramasMetadata)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NOT_FOUND)
      .hasDetailedError(s"panorama ${invalidPanoramaId} not found")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit = {
    val invalidPanoramaId = getRandomString

    val response = api.exteriorPanorama.addMetadataExt().reqSpec(defaultSpec)
      .panoramaIdPath(invalidPanoramaId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError)
      .contains("The request content was malformed:\nExpect message object but got: null")
  }

  @Test
  @Owner(TIMONDL)
  def shouldHasNoDiffWithProduction(): Unit = {
    val panoramaMetadata = new AutoPanoramasMetadata()
      .deviceInfo(random(classOf[VertisRequestContextDeviceInfo]))

    api.exteriorPanorama.addMetadataExt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(PANORAMA_ID)
      .body(panoramaMetadata)
      .execute(validatedWith(shouldBeSuccess))
  }
}
