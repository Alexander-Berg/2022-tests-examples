package ru.auto.tests.publicapi.panorama

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}
import org.assertj.core.api.Assertions
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

import java.util.Optional
import scala.annotation.meta.getter
import scala.util.Random

@DisplayName("DELETE /panorama/interior/{panorama_id}/poi/{poi_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteInteriorPoiTest {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def deletePoi(): Unit = {
    val panoramaId = "1442390104-1646915962267-L6CzS"
    adaptor.deleteAllInteriorPoi(panoramaId)

    val x1 = Random.between(1, 1000)
    val y1 = Random.between(1, 1000)
    val poi1 = adaptor.makeInteriorPoi(x1, y1, getRandomString, Optional.empty)
    val createdPoi1 = adaptor.addInteriorPoi(panoramaId, poi1)

    val x2 = Random.between(1, 1000)
    val y2 = Random.between(1, 1000)
    val poi2 = adaptor.makeInteriorPoi(x2, y2, getRandomString, Optional.empty)
    val createdPoi2 = adaptor.addInteriorPoi(panoramaId, poi2)

    api.interiorPanorama().deletePoiInt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdPath(createdPoi1.getPoint.getId)
      .execute(validatedWith(shouldBe200OkJSON))

    val poiListing1 = adaptor.getAllInteriorPoi(panoramaId)

    Assertions.assertThat(poiListing1.getPoi).hasSize(1)
    assertThat(poiListing1.getPoi.get(0).getPoint).hasId(createdPoi2.getPoint.getId)

    api.interiorPanorama().deletePoiInt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdPath(createdPoi2.getPoint.getId)
      .execute(validatedWith(shouldBe200OkJSON))

    val poiListing2 = adaptor.getAllInteriorPoi(panoramaId)

    Assertions.assertThat(poiListing2.getPoi).isNull()
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.interiorPanorama.deletePoiInt()
      .panoramaIdPath(getRandomString)
      .poiIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee404WithInvalidPanoramaId(): Unit = {
    val invalidPanoramaId = getRandomString
    api.interiorPanorama.deletePoiInt().reqSpec(defaultSpec)
      .panoramaIdPath(invalidPanoramaId)
      .poiIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  def shouldSee400WithInvalidPoiId(): Unit = {
    val panoramaId = "1637032170-1646915938724-teEhA"
    adaptor.deleteAllInteriorPoi(panoramaId)

    val invalidPoiId = getRandomString
    api.interiorPanorama.deletePoiInt().reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdPath(invalidPoiId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }
}
