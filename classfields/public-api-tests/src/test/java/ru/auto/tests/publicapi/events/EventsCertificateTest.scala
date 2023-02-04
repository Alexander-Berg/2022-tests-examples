package ru.auto.tests.publicapi.events

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
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.consts.Owners.RAIV
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model.{AutoApiErrorResponse, AutoApiReportedCertificate, VertisEventsDeviceFingerprint}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter
import scala.collection.immutable.HashMap
import scala.jdk.CollectionConverters._

@DisplayName("PUT /events/certificate")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class EventsCertificateTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(RAIV)
  def shouldSee403WhenNoAuth(): Unit = {
    api.events
      .logCertificateData()
      .body(new AutoApiReportedCertificate().domainName("auto"))
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(RAIV)
  def shouldSee400WithoutBody(): Unit = {
    val response = api.events
      .logCertificateData()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions
      .assertThat(response.getDetailedError)
      .contains("The request content was malformed:\nExpect message object but got: null")
  }

  @Test
  @Owner(RAIV)
  def shouldSuccessCreateCertificate(): Unit = {
    api.events
      .logCertificateData()
      .body(new AutoApiReportedCertificate().domainName("auto"))
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeSuccess))
  }
}
