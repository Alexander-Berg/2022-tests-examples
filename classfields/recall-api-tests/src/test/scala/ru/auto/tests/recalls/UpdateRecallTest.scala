package ru.auto.tests.recalls

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.assertj.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.adaptor.RecallApiAdaptor
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.{getRandomShortInt, getRandomString}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.recall.ApiClient
import ru.auto.tests.recall.model.{RuYandexAutoVinRecallsProtoGostCampaign, RuYandexAutoVinRecallsProtoRecall}

import scala.annotation.meta.getter

@DisplayName("PUT /recalls")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UpdateRecallTest {

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
      .changeRecall()
      .reqSpec(defaultSpec)
      .recallIdPath(getRandomString)
      .xUserIdHeader(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldUpdateRecall(): Unit = {
    val userId = getRandomString
    val recallId = adaptor.addRecall(userId).getRecall.getId
    val requestBody = new RuYandexAutoVinRecallsProtoRecall()
      .title(getRandomString)
      .description(getRandomString)
      .manufacturer(getRandomString)
      .gostCampaign(
        new RuYandexAutoVinRecallsProtoGostCampaign()
          .count(getRandomShortInt)
          .marks(getRandomString)
          .models(getRandomString)
          .url(getRandomString)
      )

    val response = api.recalls
      .changeRecall()
      .reqSpec(defaultSpec)
      .recallIdPath(recallId)
      .body(requestBody)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val recallResponse = adaptor.getRecall(response.getRecall.getId, userId)
    assertThat(recallResponse.getRecall)
      .hasTitle(requestBody.getTitle)
      .hasDescription(requestBody.getDescription)
      .hasManufacturer(requestBody.getManufacturer)
    assertThat(recallResponse.getRecall.getGostCampaign)
      .hasMarks(requestBody.getGostCampaign.getMarks)
      .hasModels(requestBody.getGostCampaign.getModels)
      .hasCount(requestBody.getGostCampaign.getCount)
      .hasUrl(requestBody.getGostCampaign.getUrl)
  }
}
