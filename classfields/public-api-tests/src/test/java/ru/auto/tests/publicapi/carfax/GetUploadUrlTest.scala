package ru.auto.tests.publicapi.carfax

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /carfax/user/comment/photo_upload_url")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetUploadUrlTest {

  private val REGEX = "^https://uploader\\.test\\.vertis\\.yandex\\.net:443/upload\\?sign=[A-Za-z0-9.]+"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.carfax.photoUploadUrlCarfax().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeeCorrectPhotoUploadUrl(): Unit = {
    val response = api.carfax.photoUploadUrlCarfax()
      .reqSpec(defaultSpec)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getUploadUrl).containsPattern(REGEX)
  }
}
