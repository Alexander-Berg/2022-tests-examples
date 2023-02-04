package ru.auto.tests.publicapi.panorama

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}
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
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import java.util.Optional
import scala.annotation.meta.getter
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Random

@DisplayName("POST /panorama/interior/{panorama_id}/poi")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PostInteriorPoiTest {

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
  def createPoi(): Unit = {
    val panoramaId = "1616847106-1645186591870-R6OO4"
    adaptor.deleteAllInteriorPoi(panoramaId)

    val x = Random.between(1, 1000)
    val y = Random.between(1, 1000)
    val text = getRandomString
    val poi = adaptor.makeInteriorPoi(x, y, text, Optional.of("https://avatars.mds.yandex.net/get-zen_doc/1705407/pub_6109ca83621f3b6b19a01a59_6109ca8e1e7e500119e38bbd/scale_1200"))

    val createdPoi = api.interiorPanorama()
      .createPoiInt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .body(poi)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val poiResponse = adaptor.getAllInteriorPoi(panoramaId)

    Assertions.assertThat(poiResponse.getPoi).hasSize(1)
    assertThat(poiResponse.getPoi.get(0).getPoint).hasId(createdPoi.getPoint.getId)
    val poiToCheck = poiResponse.getPoi.asScala.filter(_.getPoint.getId == createdPoi.getPoint.getId).head
    assertThat(poiToCheck.getPoint.getCoordinates).hasX(x)
    assertThat(poiToCheck.getPoint.getCoordinates).hasY(y)
    assertThat(poiToCheck.getProperties.getText).hasText(text)
    Assertions.assertThat(poiToCheck.getProperties.getImage).isNotNull
  }

  @Test
  def failToCreateTwoPoisWithEqualCoordinates(): Unit = {
    val panoramaId = "2049581456-1645108272748-Kqvfk"
    adaptor.deleteAllInteriorPoi(panoramaId)

    val x = Random.between(1, 1000)
    val y = Random.between(1, 1000)
    val poi = adaptor.makeInteriorPoi(x, y, getRandomString, Optional.empty)
    adaptor.addInteriorPoi(panoramaId, poi)

    val anotherPoi = adaptor.makeInteriorPoi(x, y, getRandomString, Optional.empty)
    api.interiorPanorama()
      .createPoiInt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .body(anotherPoi)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.interiorPanorama.createPoiInt()
      .panoramaIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400WithoutBody(): Unit = {
    val invalidPanoramaId = getRandomString

    api.interiorPanorama.createPoiInt().reqSpec(defaultSpec)
      .panoramaIdPath(invalidPanoramaId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def shouldSee404WithInvalidPanoramaId(): Unit = {
    val invalidPanoramaId = getRandomString
    val x = Random.between(1, 1000)
    val y = Random.between(1, 1000)
    val poi = adaptor.makeInteriorPoi(x, y, getRandomString, Optional.empty)

    api.interiorPanorama.createPoiInt().reqSpec(defaultSpec)
      .panoramaIdPath(invalidPanoramaId)
      .body(poi)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val panoramaId = "1744707979-1644834285357-SqwiE"
    adaptor.deleteAllInteriorPoi(panoramaId)

    val x = Random.between(1, 1000)
    val y = Random.between(1, 1000)
    val poi = adaptor.makeInteriorPoi(x, y, getRandomString, Optional.empty)
    adaptor.addInteriorPoi(panoramaId, poi)

    val req = (apiClient: ApiClient) => apiClient.interiorPanorama
      .getPoiInt
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
