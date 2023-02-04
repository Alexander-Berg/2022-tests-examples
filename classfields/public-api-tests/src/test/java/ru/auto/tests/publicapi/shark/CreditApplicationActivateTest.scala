package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.{Assertions, Condition}
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, validatedWith}
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.TestData

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

object CreditApplicationActivateTest {

  @Parameterized.Parameters(name = "{0}")
  def getParameters: Array[Array[AnyRef]] =
    Array(TestData.defaultCreditApplications())
}

@DisplayName("PUT /shark/credit-application/activate/{credit_application_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class CreditApplicationActivateTest(val creditApplication: String) {

  @(Rule@getter)
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
  def shouldActivateCreditApplication(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val creditApplicationId = adaptor.createCreditApplication(sessionId, creditApplication).getCreditApplication.getId

    val userSettings = new VertisSharkCreditApplicationUserSettings()
      .affiliateUserId("affiliate-user-id")
      .tags(Seq("tag-1", "tag-2").asJava)

    api.shark.activate()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(creditApplicationId)
      .body(new VertisSharkApiActivationRequest().userSettings(userSettings))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val creditApplicationAfterChange = adaptor.getCreditApplication(sessionId, creditApplicationId).getCreditApplication

    Assertions.assertThat(creditApplicationAfterChange.getState).isEqualTo(VertisSharkCreditApplication.StateEnum.ACTIVE)
    Assertions.assertThat(creditApplicationAfterChange.getUserSettings).isEqualTo(userSettings)
    Assertions.assertThat(creditApplicationAfterChange.getClaims).haveAtLeast(3, new Condition[VertisSharkCreditApplicationClaim]() {
      override def matches(value: VertisSharkCreditApplicationClaim): Boolean = true
    })
  }
}
