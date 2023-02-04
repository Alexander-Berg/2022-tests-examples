package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("POST /shark/credit-application/user-settings")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UserSettingsCreditApplicationTest {

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
  def setAffiliateUserIdToUserSettings(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val response = {
      val body = new AutoApiSharkUserSettingsRequest
      body.setAffiliateUserId("5")
      api.shark.userSettings()
        .reqSpec(defaultSpec)
        .body(body)
        .xSessionIdHeader(sessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))
      api.shark.creditApplicationCreate()
        .reqSpec(defaultSpec)
        .body(new VertisSharkCreditApplicationSource)
        .xSessionIdHeader(sessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))
    }
    val creditApplication = adaptor.getCreditApplication(sessionId, response.getCreditApplication.getId)
    val expectedUserSettings = {
      val obj = new VertisSharkCreditApplicationUserSettings
      obj.setAffiliateUserId("5")
      obj
    }
    assertThat(creditApplication.getCreditApplication).hasUserSettings(expectedUserSettings)
  }

  @Test
  @Owner(SHARK)
  def updateTags(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val userSettingsBody = new AutoApiSharkUserSettingsRequest
    userSettingsBody.setAffiliateUserId("5")
    api.shark.userSettings()
      .reqSpec(defaultSpec)
      .body(userSettingsBody)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val creditApplicationSource = {
      val obj = new VertisSharkCreditApplicationSource
      val userSettings = new VertisSharkCreditApplicationUserSettings
      userSettings.setAffiliateUserId("10")
      userSettings.setTags(Seq("a", "b").asJava)
      obj.setUserSettings(userSettings)
      obj
    }

    val response =
      api.shark.creditApplicationCreate()
        .reqSpec(defaultSpec)
        .body(creditApplicationSource)
        .xSessionIdHeader(sessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))
    val id = response.getCreditApplication.getId

    val anotherUserSettingsBody = new AutoApiSharkUserSettingsRequest
    anotherUserSettingsBody.setAffiliateUserId("15")
    api.shark.userSettings()
      .reqSpec(defaultSpec)
      .body(anotherUserSettingsBody)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val updateWithUserSettings = new VertisSharkCreditApplicationUserSettings
    updateWithUserSettings.setTags(Seq("b", "c").asJava)
    api.shark.creditApplicationUpdate()
      .creditApplicationIdPath(id)
      .reqSpec(defaultSpec)
      .body(creditApplicationSource.userSettings(updateWithUserSettings))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val creditApplication = adaptor.getCreditApplication(sessionId, id)
    val expectedUserSettings = {
      val obj = new VertisSharkCreditApplicationUserSettings
      obj.setAffiliateUserId("5")
      obj.setTags(Seq("a", "b", "c").asJava)
      obj
    }
    assertThat(creditApplication.getCreditApplication).hasUserSettings(expectedUserSettings)
  }
}
