package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.model.VertisSharkCreditApplication.{StateEnum => ResultStateEnum}
import ru.auto.tests.publicapi.model.VertisSharkCreditApplicationSource.{StateEnum => SourceStateEnum}
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

object ChangeCreditApplicationStatusTest {
  @Parameterized.Parameters(name = "{index}: {0}")
  def getParameters: Array[Array[AnyRef]] = Array(
    Array(SourceStateEnum.ACTIVE, ResultStateEnum.ACTIVE),
    Array(SourceStateEnum.CANCELED, ResultStateEnum.CANCELED)
  )
}

@DisplayName("POST /shark/credit-application/update/{credit_application_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class ChangeCreditApplicationStatusTest(sourceState: SourceStateEnum, expectedState: ResultStateEnum) {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(SHARK)
  def shouldGetMigrationInfoHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val creditApplicationId = adaptor.createCreditApplication(sessionId).getCreditApplication.getId

    val response = api.shark.creditApplicationUpdate()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(creditApplicationId)
      .body(new VertisSharkCreditApplicationSource().state(sourceState))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getCreditApplication).hasState(expectedState)
  }
}
