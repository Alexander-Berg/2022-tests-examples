package ru.auto.tests.publicapi.feeds

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.{MatcherAssert, Matchers}
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.passport.account.Account
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.{PublicApiAdaptor, PublicApiDealerFeedsAdaptor}
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.feeds.FeedsCreateTrucksTaskTest.TestAccount
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{CategoryEnum, SectionEnum, TruckCategoryEnum}
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedFeedTask.{StatusEnum, TypeEnum}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.operations.feeds.settings.feedSettings
import ru.auto.tests.publicapi.operations.feeds.tasks.{GetFeedsHistoryOps, taskSettings}
import ru.auto.tests.publicapi.operations.feeds.tasks.trucks.CreateTrucksFeedsTaskOps
import ru.auto.tests.publicapi.operations.feeds.RichFeedsCollection
import ru.auto.tests.utils.Authorization
import ru.auto.tests.utils.ParameterizedUtils.parameterize

import scala.annotation.meta.getter

@DisplayName("POST /feeds/task/trucks/{trucks_category}/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class FeedsCreateTrucksTaskTest(category: TruckCategoryEnum, section: SectionEnum, feedSettingsCheckboxValue: Boolean)
  extends CreateTrucksFeedsTaskOps with GetFeedsHistoryOps with Authorization {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  override val api: ApiClient = null

  @Inject
  override val adaptor: PublicApiAdaptor = null

  @Inject
  val feedsAdaptor: PublicApiDealerFeedsAdaptor = null

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccess(): Unit = {
    val sessionId = getSessionId(TestAccount)

    val feedUrl = s"feed_${category.name}_${section.name}.xml"

    val testFeedSettings =
      feedSettings(feedUrl)
        .deleteSale(feedSettingsCheckboxValue)
        .leaveServices(feedSettingsCheckboxValue)
        .leaveAddedImages(feedSettingsCheckboxValue)
        .maxDiscountEnabled(feedSettingsCheckboxValue)
        .isActive(null)

    val testTaskSettings = taskSettings(
      internalUrl = "internal_" + feedUrl,
      settings = testFeedSettings
    )

    val result = createTrucksTask(testTaskSettings, category, section)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val taskId = result.getId

    MatcherAssert.assertThat(taskId, Matchers.notNullValue)

    val history = feedsAdaptor.getHistory()
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val lastTask = history.getFeedsList.head

    MatcherAssert.assertThat(lastTask.getTask.getId, Matchers.equalTo(taskId))
    MatcherAssert.assertThat(lastTask.getTask.getSettings, Matchers.equalTo(testTaskSettings))
    MatcherAssert.assertThat(lastTask.getTask.getType, Matchers.equalTo(TypeEnum.MANUAL))
    MatcherAssert.assertThat(lastTask.getTask.getStatus, Matchers.equalTo(StatusEnum.NEW))
    MatcherAssert.assertThat(lastTask.getCategory.getCategory, Matchers.equalTo(CategoryEnum.TRUCKS))
    MatcherAssert.assertThat(lastTask.getCategory.getTruckCategory, Matchers.equalTo(category))
    MatcherAssert.assertThat(lastTask.getCategory.getSection, Matchers.equalTo(section))
  }

}

object FeedsCreateTrucksTaskTest {

  private val TestAccount = Account.builder()
    .login("FeedsCreateTrucksTaskTest@regress.apiauto.ru")
    .password("autoru")
    .id("70519766")
    .build()

  @Parameterized.Parameters(name = "category = {0}, section = {1}, feedSettingsCheckboxValue = {2}")
  def parameters = parameterize {
    for {
      category <- TruckCategoryEnum.values()
      section <- SectionEnum.values()
      feedSettingsCheckboxValue <- Seq(true, false)
    } yield (category, section, feedSettingsCheckboxValue)
  }

}
