package ru.auto.tests.publicapi.device

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter


@DisplayName("POST /device/init")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class InitNegativeTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = api.device.init.execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit = api.device.init.reqSpec(defaultSpec)
    .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
}
