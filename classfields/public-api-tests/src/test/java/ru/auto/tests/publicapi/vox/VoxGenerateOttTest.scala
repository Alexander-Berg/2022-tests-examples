package ru.auto.tests.publicapi.vox

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /vox/generate-ott/")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class VoxGenerateOttTest {
  import VoxGenerateOttTest._

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
  def shouldSuccessGenerateOtt(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val result = api.vox().generateOtt().reqSpec(defaultSpec)
      .loginKeyQuery(LoginKey)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))
    Assertions.assertThat(result.getToken).isNotEmpty()
  }
}

object VoxGenerateOttTest {
  val LoginKey = "a1"
}
