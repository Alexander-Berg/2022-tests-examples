package ru.auto.tests.publicapi.panorama

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /panorama/interior/upload")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetInteriorPanoramaUploadUrlTest {

  private val SINGLE_UPLOAD = "https://uploader.test.vertis.yandex.net:443/upload?sign="
  private val MULTIPART_CREATE = "https://uploader.test.vertis.yandex.net:443/multi-part/create?sign="
  private val MULTIPART_UPLOAD = "https://uploader.test.vertis.yandex.net:443/multi-part/upload?sign="
  private val MULTIPART_COMPLETE = "https://uploader.test.vertis.yandex.net:443/multi-part/complete?sign="

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.interiorPanorama.uploadInt()
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldHasAllUrls(): Unit = {
    val response = api.interiorPanorama.uploadInt()
      .reqSpec(defaultSpec)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getSign).isNotEmpty
    assertThat(response.getUploadUrl).startsWith(SINGLE_UPLOAD)
    assertThat(response.getMultiPartUploadUrls).isNotNull
    assertThat(response.getMultiPartUploadUrls.getCreateUrl).startsWith(MULTIPART_CREATE)
    assertThat(response.getMultiPartUploadUrls.getUploadUrl).startsWith(MULTIPART_UPLOAD)
    assertThat(response.getMultiPartUploadUrls.getConfirmUrl).startsWith(MULTIPART_COMPLETE)
  }
}
