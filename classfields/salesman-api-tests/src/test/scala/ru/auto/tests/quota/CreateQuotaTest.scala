package ru.auto.tests.quota

import java.time.OffsetDateTime

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.common.collect.Lists.newArrayList
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.model.{QuotaRequestView, QuotaSettings}
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /service/{service}/quota")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateQuotaTest {

  private val CLIENT_ID = "20101"
  private val QUOTA_TYPE = "quota:placement:cars:used"
  private val SETTINGS = new QuotaSettings().size(1L).entity("dealer")

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee200AddQuota(): Unit =
    api.quota
      .addQuotaRequestRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .body(
        newArrayList(
          new QuotaRequestView()
            .client(CLIENT_ID)
            .quotaType(QUOTA_TYPE)
            .from(OffsetDateTime.now)
            .settings(SETTINGS)
        )
      )
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
}
