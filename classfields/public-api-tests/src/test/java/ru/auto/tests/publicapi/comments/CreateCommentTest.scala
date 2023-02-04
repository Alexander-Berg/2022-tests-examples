package ru.auto.tests.publicapi.comments

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{After, Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiAddCommentRequest
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /comments/{topicGroup}/{topicId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateCommentTest {

  private val TOPIC_GROUP = "review"
  private val TOPIC_ID = "8498572144589540098"

  private var commentId: String = _
  private var sessionId: String = _

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
  def shouldAddNewComment(): Unit = {
    val account = accountManager.create()
    sessionId = adaptor.login(account).getSession.getId
    val message = getRandomString

    val addResponse = api.comments.addComment().reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .body(new AutoApiAddCommentRequest().message(message))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    commentId = addResponse.getComment.getId

    val commentsResponse = api.comments.getComments.reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val myComment = commentsResponse.getComments.stream
      .filter(comment => comment.getId.equals(commentId))
      .findFirst()

    Assertions.assertThat(myComment).isNotEmpty
    Assertions.assertThat(myComment.get.getId).isEqualTo(commentId)
    Assertions.assertThat(myComment.get.getMessage).isEqualTo(message)
  }

  @After
  def deleteComment(): Unit = {
    api.comments.deleteComment().reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .commentIdPath(commentId)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }
}
