package ru.auto.tests.publicapi.events

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.model.AutoApiEventsReportRequest
import ru.auto.tests.publicapi.module.PublicApiModule
import org.apache.http.HttpStatus.SC_FORBIDDEN
import ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("POST /events/log")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class CreateEventsTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.events.logEvents()
      .body(new AutoApiEventsReportRequest)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee200WithEmptyBody(): Unit = {
    api.events.logEvents().reqSpec(defaultSpec)
      .body(new AutoApiEventsReportRequest)
      .execute(validatedWith(shouldBeSuccess))
  }
}
