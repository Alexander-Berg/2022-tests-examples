package ru.auto.tests.publicapi.calltracking

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{After, Rule, Test}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.{AutoApiCalltrackingAddTagRequest, AutoCalltrackingCallTag}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount

import scala.annotation.meta.getter

@DisplayName("PUT /calltracking/call/tag")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class AddTagToCallTest {

  private var sessionId: String = _
  private val callId = 1199159L
  private val tag = getRandomString

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSuccessUpdateSettings(): Unit = {
    sessionId = adaptor.login(getDemoAccount).getSession.getId
    val requestBody = new AutoApiCalltrackingAddTagRequest().callId(callId)
      .addTagsItem(new AutoCalltrackingCallTag().value(tag))

    api.calltracking.addCallTag().reqSpec(defaultSpec)
      .body(requestBody)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeSuccess))
  }

  @After
  def deleteTag(): Unit = {
    adaptor.removeTagFromCall(sessionId, callId, tag)
  }
}
