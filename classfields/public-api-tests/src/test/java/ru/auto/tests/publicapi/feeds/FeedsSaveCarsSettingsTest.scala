package ru.auto.tests.publicapi.feeds

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import org.hamcrest.{MatcherAssert, Matchers}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.passport.account.Account
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.feeds.FeedsSaveCarsSettingsTest.TestAccount
import ru.auto.tests.utils.ParameterizedUtils.parameterize
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{CategoryEnum, SectionEnum}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.utils.Authorization
import ru.auto.tests.publicapi.operations.feeds.RichFeedsCollection
import ru.auto.tests.publicapi.operations.feeds.settings.cars.{DeleteCarsFeedsSettingsOps, SaveCarsFeedsSettingsOps}
import ru.auto.tests.publicapi.operations.feeds.settings.GetFeedsSettingsOps
import ru.auto.tests.publicapi.operations.feeds.settings._

import scala.annotation.meta.getter

@DisplayName("POST /feeds/settings/cars/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class FeedsSaveCarsSettingsTest(section: SectionEnum, feedSettingsCheckboxValue: Boolean)
  extends SaveCarsFeedsSettingsOps with GetFeedsSettingsOps with DeleteCarsFeedsSettingsOps with Authorization {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  override val api: ApiClient = null

  @Inject
  override val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(DEALER_PRODUCTS)
  def shouldSeeSuccess(): Unit = {
    val sessionId = getSessionId(TestAccount)

    deleteCarsSettings(section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val settings = {
      feedSettings(s"feed_${section.name.toLowerCase}.xml")
        .isActive(feedSettingsCheckboxValue)
        .deleteSale(feedSettingsCheckboxValue)
        .leaveServices(feedSettingsCheckboxValue)
        .maxDiscountEnabled(feedSettingsCheckboxValue)
        .leaveAddedImages(feedSettingsCheckboxValue)
    }

    saveCarsSettings(settings, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val allActualSettings = getSettings()
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val actualSettings = allActualSettings.filterSettings { feed =>
      feed.getCategory == CategoryEnum.CARS && feed.getSection == section
    }

    MatcherAssert.assertThat(actualSettings, Matchers.equalTo(List(settings)))
  }

}

object FeedsSaveCarsSettingsTest {

  private val TestAccount = Account.builder()
    .login("FeedsSaveCarsSettingsTest@regress.apiauto.ru")
    .password("autoru")
    .id("70516198")
    .build()

  @Parameterized.Parameters(name = "section = {0}, feedSettingsCheckboxValue = {1}")
  def parameters = parameterize {
    for {
      section <- SectionEnum.values()
      feedSettingsCheckboxValue <- Seq(true, false)
    } yield (section, feedSettingsCheckboxValue)
  }

}
