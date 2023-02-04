package ru.auto.tests.recalls

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Before, Rule, Test}
import ru.auto.tests.adaptor.RecallApiAdaptor
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, validatedWith}
import ru.auto.tests.commons.util.Utils.{getRandomShortLong, getRandomString}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.recall.ApiClient

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

@DisplayName("PUT /recalls/{recall_id}/documents")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[GuiceTestRunner])
class AttachDocumentToRecallTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: RecallApiAdaptor = null

  private val UserId = s"""${getRandomString}_api_tes"""

  @Test
  @Owner(TIMONDL)
  def shouldAttachDocumentToRecall(): Unit = {

    val url = s"https://www.gost.ru/documentManager/rest/file/load/$getRandomString"
    val recallId = adaptor.addRecall(UserId).getRecall.getId

    api.recalls
      .recallAttachDocuments()
      .reqSpec(defaultSpec)
      .recallIdPath(recallId)
      .urlQuery(url)
      .xUserIdHeader(UserId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val recallDocuments = adaptor.getRecallDocuments(recallId, UserId)

    assertThat(recallDocuments.getDocuments.asScala.filter(_.getUrl == url).head).hasUrl(url)
  }
}
