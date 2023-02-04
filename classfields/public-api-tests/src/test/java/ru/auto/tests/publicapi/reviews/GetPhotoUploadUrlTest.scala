package ru.auto.tests.publicapi.reviews

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO
import ru.auto.tests.publicapi.model.AutoApiReviewUploadUrlResponse
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /reviews/{subject}/photo/upload_url")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetPhotoUploadUrlTest {

  private val UPLOAD_URL_PREFIX = "https://uploader.test.vertis.yandex.net:443/upload?sign="

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.reviews.photoUploadUrlReview()
      .subjectPath(AUTO)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400WithInvalidSubject(): Unit = {
    api.reviews.photoUploadUrlReview().reqSpec(defaultSpec)
      .subjectPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetMigrationInfoHasNoDiffWithProduction(): Unit = {
    val response = api.reviews.photoUploadUrlReview()
      .reqSpec(defaultSpec)
      .subjectPath(AUTO)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response).hasStatus(AutoApiReviewUploadUrlResponse.StatusEnum.SUCCESS)
    Assertions.assertThat(response.getUploadUrl).startsWith(UPLOAD_URL_PREFIX)
  }
}
