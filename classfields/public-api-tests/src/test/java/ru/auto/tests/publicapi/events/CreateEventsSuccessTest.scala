package ru.auto.tests.publicapi.events

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import io.restassured.builder.RequestSpecBuilder
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.commons.util.Utils.getResourceAsString
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.{defaultSpec, withJsonBody}
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess
import ru.auto.tests.publicapi.testdata.TestData.defaultEvents

import scala.annotation.meta.getter

object CreateEventsSuccessTest {
  @Parameterized.Parameters(name = "events={0}")
  def getParameters: Array[AnyRef] = defaultEvents
}

@DisplayName("POST /events/log")
@RunWith(classOf[Parameterized])
@GuiceModules(Array(classOf[PublicApiModule]))
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class CreateEventsSuccessTest(events: String) {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test def shouldSuccessCreateEvents(): Unit = {
    val body = getResourceAsString(events)

    api.events.logEvents.reqSpec(defaultSpec)
      .reqSpec((r: RequestSpecBuilder) => r.setBody(withJsonBody(body)))
      .execute(validatedWith(shouldBeSuccess))
  }
}
