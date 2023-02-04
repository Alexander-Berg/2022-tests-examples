package ru.auto.tests.recalls

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.recall.ApiClient

import scala.annotation.meta.getter

@DisplayName("PUT /recalls/{recall_id}/documents")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[GuiceTestRunner])
class AttachDocumentToRecallNegativeTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidRecallId(): Unit = {
    api.recalls
      .recallAttachDocuments()
      .reqSpec(defaultSpec)
      .recallIdPath(getRandomString)
      .xUserIdHeader(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }
}
