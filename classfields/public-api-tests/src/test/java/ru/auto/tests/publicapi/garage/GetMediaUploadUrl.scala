package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /garage/user/media/upload_url")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetMediaUploadUrl {

  private val REGEX = "^https://uploader\\.test\\.vertis\\.yandex\\.net:443/upload\\?sign=[A-Za-z0-9.]+"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(CARFAX)
  def shouldSeeCorrectPhotoUploadUrl(): Unit = {
    val response = api
      .garage()
      .getUploadUrl
      .reqSpec(defaultSpec)
      .uploadDataTypeQuery("CAR_PHOTO")
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getUploadUrl).containsPattern(REGEX)
  }

  @Test
  @Owner(CARFAX)
  def shouldSeeUnsupportedUploadDataType(): Unit = {
    api
      .garage()
      .getUploadUrl
      .reqSpec(defaultSpec)
      .uploadDataTypeQuery("ABC")
      .executeAs(validatedWith(shouldBeCode(400)))
  }

}
