package ru.auto.tests.publicapi.panorama

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND}
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /panorama/interior/{panorama_id}/poi/upload")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetInteriorPoiUploadUrlTest {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def getPoiUploadUrl(): Unit = {
    val panoramaId = "1405231327-1646253539037-HxoH4"
    adaptor.deleteAllInteriorPoi(panoramaId)

    val response = api.interiorPanorama()
      .uploadPoiInt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    Assertions.assertThat(response.getSign).isNotEmpty
    Assertions.assertThat(response.getUploadUrl).isNotEmpty
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.interiorPanorama.getPoiInt
      .panoramaIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee404WithInvalidPanoramaId(): Unit = {
    val invalidPanoramaId = getRandomString

    api.interiorPanorama.getPoiInt.reqSpec(defaultSpec)
      .panoramaIdPath(invalidPanoramaId)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }
}
