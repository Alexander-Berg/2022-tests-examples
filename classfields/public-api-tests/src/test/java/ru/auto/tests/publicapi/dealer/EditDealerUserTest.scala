package ru.auto.tests.publicapi.dealer

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{After, Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomEmail
import ru.auto.tests.passport.account.Account
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount

import scala.annotation.meta.getter

@DisplayName("PUT /dealer/user/{user_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class EditDealerUserTest {

  private val GROUP_ID = 8
  private var userAccount: Account = _
  private var dealerSessionId: String = _

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
  @Owner(TIMONDL)
  def shouldSeeSuccess(): Unit = {
    val userEmail = getRandomEmail
    userAccount = accountManager.create()
    dealerSessionId = adaptor.login(getDemoAccount).getSession.getId
    val userSessionId = adaptor.login(userAccount).getSession.getId
    adaptor.addEmailToUser(userSessionId, userAccount.getId, userEmail)
    adaptor.linkUserToDealer(dealerSessionId, 1, userEmail)

    val response = api.dealer.editUser().reqSpec(defaultSpec)
      .userIdPath(userAccount.getId)
      .groupQuery(GROUP_ID)
      .xSessionIdHeader(dealerSessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getAccess.getGroup).hasId(GROUP_ID)
  }

  @After
  def unlinkUser(): Unit = {
    api.dealer.unlinkUser.reqSpec(defaultSpec)
      .xSessionIdHeader(dealerSessionId)
      .userIdPath(userAccount.getId)
      .execute(validatedWith(shouldBe200OkJSON))
  }
}
