package ru.auto.tests.publicapi.events

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.model.{AutoApiErrorResponse, AutoApiEventsReportRequest}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.consts.Owners.TIMONDL

import scala.annotation.meta.getter

@DisplayName("PUT /events/facts/{event-category}/{event-type}")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class CreateFactsTest {

  private val CATEGORY = "offer"
  private val TYPE = "create"
  private val ID = "123"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.events.logFacts()
      .eventCategoryPath(CATEGORY)
      .eventTypePath(TYPE)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutIdQueryParameter(): Unit = {
    val response = api.events.logFacts().reqSpec(defaultSpec)
      .eventCategoryPath(CATEGORY)
      .eventTypePath(TYPE)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError).contains("Request is missing required query parameter 'id'")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSuccessCreateEventsFact(): Unit = {
    api.events.logFacts().reqSpec(defaultSpec)
      .eventCategoryPath(CATEGORY)
      .eventTypePath(TYPE)
      .idQuery(ID)
      .execute(validatedWith(shouldBeSuccess))
  }
}
