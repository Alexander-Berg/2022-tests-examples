package ru.auto.tests.publicapi.panorama

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}
import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers.equalTo
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.safe_deal.SafeDealFlowTest.waitUntil

import java.util.Optional
import scala.annotation.meta.getter
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Random

@DisplayName("PUT /panorama/interior/{panorama_id}/poi")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PutInteriorPoiTest {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def updatePoi(): Unit = {
    val panoramaId = "2420122259-1644588862317-DyuCa"
    adaptor.deleteAllInteriorPoi(panoramaId)

    val x1 = Random.between(1, 1000)
    val y1 = Random.between(1, 1000)
    val poi = adaptor.makeInteriorPoi(x1, y1, getRandomString, Optional.empty)
    val createdPoi = adaptor.addInteriorPoi(panoramaId, poi)

    val x2 = Random.between(1, 1000)
    val y2 = Random.between(1, 1000)
    val text2 = getRandomString
    val poiToUpdate = adaptor.makeInteriorPoi(x2, y2, text2, Optional.empty)
    api.interiorPanorama()
      .updatePoiInt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdPath(createdPoi.getPoint.getId)
      .body(poiToUpdate)
      .executeAs(validatedWith(shouldBe200OkJSON))

    waitUntil(
      () => adaptor.getAllInteriorPoi(panoramaId).getPoi.get(0).getPoint.getCoordinates.getX,
      equalTo(Double.box(x2)))

    val poiResponse = adaptor.getAllInteriorPoi(panoramaId)

    Assertions.assertThat(poiResponse.getPoi).hasSize(1)
    assertThat(poiResponse.getPoi.get(0).getPoint).hasId(createdPoi.getPoint.getId)
    val updatedPoi = poiResponse.getPoi.asScala.filter(_.getPoint.getId == createdPoi.getPoint.getId).head
    assertThat(updatedPoi.getPoint.getCoordinates).hasX(x2)
    assertThat(updatedPoi.getPoint.getCoordinates).hasY(y2)
    assertThat(updatedPoi.getProperties.getText).hasText(text2)
  }

  @Test
  def failToUpdateNonExistingPoi(): Unit = {
    val panoramaId = "1936073159-1644441492653-eWy98"
    adaptor.deleteAllInteriorPoi(panoramaId)

    val x = Random.between(1, 1000)
    val y = Random.between(1, 1000)
    val poiToUpdate = adaptor.makeInteriorPoi(x, y, getRandomString, Optional.empty)
    val nonExistingPoiId = getRandomString
    api.interiorPanorama()
      .updatePoiInt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdPath(nonExistingPoiId)
      .body(poiToUpdate)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.interiorPanorama.updatePoiInt()
      .panoramaIdPath(getRandomString)
      .poiIdPath(getRandomString)
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
}
