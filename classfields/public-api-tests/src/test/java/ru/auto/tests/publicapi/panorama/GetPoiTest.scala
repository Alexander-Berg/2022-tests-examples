package ru.auto.tests.publicapi.panorama

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND}
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
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import java.util.Optional
import scala.annotation.meta.getter
import scala.util.Random

@DisplayName("GET /panorama/{panorama_id}/poi")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetPoiTest {

  @(Rule@getter)
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
  def getNonExistingPoi(): Unit = {
    val panoramaId = "2094801476-1653307232747-aUf8E"
    adaptor.deleteAllExteriorPoi(panoramaId)

    val res = api.exteriorPanorama()
      .getPoiExt
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .executeAs(validatedWith(shouldBe200OkJSON))
    Assertions.assertThat(res.getPoi).isNull()
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.exteriorPanorama.getPoiExt
      .panoramaIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee404WithInvalidPanoramaId(): Unit = {
    val invalidPanoramaId = getRandomString

    api.exteriorPanorama.getPoiExt.reqSpec(defaultSpec)
      .panoramaIdPath(invalidPanoramaId)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val panoramaId = "1725636944-1653302612354-PIkoO"
    adaptor.deleteAllExteriorPoi(panoramaId)

    val x1 = Random.between(1, 1000)
    val y1 = Random.between(1, 1000)
    val poi1 = adaptor.makeExteriorPoi(x1, y1, getRandomString, Optional.empty)
    adaptor.addExteriorPoi(panoramaId, poi1)

    val x2 = Random.between(1, 1000)
    val y2 = Random.between(1, 1000)
    val poi2 = adaptor.makeExteriorPoi(x2, y2, getRandomString, Optional.empty)
    adaptor.addExteriorPoi(panoramaId, poi2)

    val req = (apiClient: ApiClient) => apiClient.exteriorPanorama
      .getPoiExt
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
