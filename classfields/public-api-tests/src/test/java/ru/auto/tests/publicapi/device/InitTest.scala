package ru.auto.tests.publicapi.device

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.model.AutoApiDevice.PlatformEnum
import ru.auto.tests.publicapi.model.AutoApiDevice.PlatformEnum.{ANDROID, IOS}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomHelloRequest
import ru.auto.tests.publicapi.consts.Owners.TIMONDL

import scala.annotation.meta.getter

object InitTest {
  @Parameterized.Parameters(name = "{index}: {0}")
  def getParameters: Array[PlatformEnum] = Array(ANDROID, IOS)
}

@DisplayName("POST /device/init")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class InitTest(platform: PlatformEnum) {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee200Ok(): Unit = api.device.init.reqSpec(defaultSpec)
    .body(getRandomHelloRequest(platform))
    .execute(validatedWith(shouldBe200OkJSON))
}
