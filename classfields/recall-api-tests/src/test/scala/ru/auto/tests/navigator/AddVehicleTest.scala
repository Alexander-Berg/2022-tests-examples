package ru.auto.tests.navigator

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.recall.ApiClient
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /navigator/vehicle")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[GuiceTestRunner])
class AddVehicleTest {

  private val VIN_CODE_ID = 23706

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithoutVinCodeId(): Unit = {
    api.navigator
      .addVehicle()
      .reqSpec(defaultSpec)
      .xUserIdHeader(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldAddVehicle(): Unit = {
    api.navigator
      .addVehicle()
      .reqSpec(defaultSpec)
      .vinCodeIdQuery(VIN_CODE_ID)
      .subscribeQuery(false)
      .xUserIdHeader(getRandomString)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }
}
