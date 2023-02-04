package ru.auto.tests.publicapi.panorama

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_INTERNAL_SERVER_ERROR}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.{getRandomShortInt, getRandomString}
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import java.util.Optional
import scala.annotation.meta.getter
import scala.util.Random

@DisplayName("POST /panorama/{panorama_id}/poi/upload/callback")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PostPoiCallbackTest {

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
  def shouldSee403WhenNoAuth(): Unit = {
    api.exteriorPanorama
      .callbackPoiExt()
      .panoramaIdPath(getRandomString)
      .poiIdQuery(getRandomString)
      .body(new UploadResponse())
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldFailOnInvalidPoiId(): Unit = {
    val panoramaId = "1512775879-1651653481538-2VCuA"
    adaptor.deleteAllExteriorPoi(panoramaId)

    api.exteriorPanorama.callbackPoiExt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdQuery(getRandomString)
      .body(new UploadResponse().name(getRandomString)
        .namespace(getRandomString)
        .groupId(getRandomShortInt)
        .url(getRandomString))
      .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)))
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val panoramaId = "1759060383-1649836283889-iT6in"
    adaptor.deleteAllExteriorPoi(panoramaId)

    val x = Random.between(1, 1000)
    val y = Random.between(1, 1000)
    val poi = adaptor.makeExteriorPoi(x, y, getRandomString, Optional.of("https://avatars.mds.yandex.net/get-zen_doc/1705407/pub_6109ca83621f3b6b19a01a59_6109ca8e1e7e500119e38bbd/scale_1200"))
    val createdPoi = adaptor.addExteriorPoi(panoramaId, poi)

    val link = createdPoi.getProperties.getImage.get(0).getLink
    val partsCount = link.count(_ == '/') + 1
    val parts = link.split("/", partsCount)
    val name = parts(partsCount - 2)
    val groupId = parts(partsCount - 3)
    val namespace = parts(partsCount - 4)

    val req = (apiClient: ApiClient) => apiClient.exteriorPanorama
      .callbackPoiExt()
      .reqSpec(defaultSpec)
      .panoramaIdPath(panoramaId)
      .poiIdQuery(createdPoi.getPoint.getId)
      .body(new UploadResponse()
        .name(name)
        .namespace(namespace)
        .groupId(groupId.toInt)
        .url(link)
      )
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
