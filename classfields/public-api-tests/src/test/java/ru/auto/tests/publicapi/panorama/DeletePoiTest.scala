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

@DisplayName("DELETE /panorama/{panorama_id}/poi/{poi_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeletePoiTest {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def deletePoi(): Unit = {
    val panoramaId = "1904615738-1655301527013-DTwgi"
    adaptor.deleteAllExteriorPoi(panoramaId)

    val x1 = Random.between(1, 1000)
    val y1 = Random.between(1, 1000)
    val poi1 = adaptor.makeExteriorPoi(x1, y1, getRandomString, Optional.empty)
    val createdPoi1 = adaptor.addExteriorPoi(panoramaId, poi1)

    val x2 = Random.between(1, 1000)
    val y2 = Random.between(1, 1000)
    val poi2 = adaptor.makeExteriorPoi(x2, y2, getRandomString, Optional.empty)
    val createdPoi2 = adaptor.addExteriorPoi(panoramaId, poi2)

    api.exteriorPanorama().deletePoiExt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdPath(createdPoi1.getPoint.getId)
      .execute(validatedWith(shouldBe200OkJSON))

    val poiListing1 = adaptor.getAllExteriorPoi(panoramaId)

    Assertions.assertThat(poiListing1.getPoi).hasSize(1)
    assertThat(poiListing1.getPoi.get(0).getPoint).hasId(createdPoi2.getPoint.getId)

    api.exteriorPanorama().deletePoiExt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdPath(createdPoi2.getPoint.getId)
      .execute(validatedWith(shouldBe200OkJSON))

    val poiListing2 = adaptor.getAllExteriorPoi(panoramaId)

    Assertions.assertThat(poiListing2.getPoi).isNull()
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.exteriorPanorama.deletePoiExt()
      .panoramaIdPath(getRandomString)
      .poiIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee404WithInvalidPanoramaId(): Unit = {
    val invalidPanoramaId = getRandomString
    api.exteriorPanorama.deletePoiExt().reqSpec(defaultSpec)
      .panoramaIdPath(invalidPanoramaId)
      .poiIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  def shouldSee400WithInvalidPoiId(): Unit = {
    val panoramaId = "2308252228-1654688821211-HBSyT"
    adaptor.deleteAllExteriorPoi(panoramaId)

    val invalidPoiId = getRandomString
    api.exteriorPanorama.deletePoiExt().reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdPath(invalidPoiId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }
}
