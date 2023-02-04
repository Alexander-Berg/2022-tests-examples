package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, validatedWith}
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.TestData

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._


object CreditApplicationAddProductsTest {

  @Parameterized.Parameters(name = "{0}")
  def getParameters: Array[Array[AnyRef]] =
    Array(TestData.defaultCreditApplications())
}

@DisplayName("PUT /shark/credit-application/add-products/{credit_application_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class CreditApplicationAddProductsTest(val creditApplication: String) {

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
  def shouldAddProductsToCreditApplication(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val creditApplicationId = adaptor.createCreditApplication(sessionId, creditApplication).getCreditApplication.getId

    api.shark.addProducts()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(creditApplicationId)
      .creditProductIdsQuery(TestProductId)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val creditApplicationAfterChange = adaptor.getCreditApplication(sessionId, creditApplicationId)
    val creditApplicationProductIds = creditApplicationAfterChange.getCreditApplication.getClaims.asScala.map(_.getCreditProductId)
    Assertions.assertThat(creditApplicationProductIds.asJava).containsExactlyInAnyOrder(TestProductId)
  }
}
