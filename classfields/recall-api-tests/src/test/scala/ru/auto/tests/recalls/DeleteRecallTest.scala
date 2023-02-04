package ru.auto.tests.recalls

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.adaptor.RecallApiAdaptor
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200Ok, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.recall.ApiClient
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.acceptText

import scala.annotation.meta.getter

@DisplayName("DELETE /recalls/{recall_id}")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteRecallTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: RecallApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidRecallId(): Unit = {
    api.recalls
      .deleteRecall()
      .reqSpec(acceptText)
      .recallIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldDeleteRecall(): Unit = {
    val userId = getRandomString
    val recallId = adaptor.addRecall(userId).getRecall.getId

    api.recalls
      .deleteRecall()
      .reqSpec(acceptText)
      .recallIdPath(recallId)
      .xUserIdHeader(userId)
      .execute(validatedWith(shouldBe200Ok))
  }
}
