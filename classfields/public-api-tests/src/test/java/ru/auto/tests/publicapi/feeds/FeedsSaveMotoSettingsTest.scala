package ru.auto.tests.publicapi.feeds

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.{MatcherAssert, Matchers}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.passport.account.Account
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.feeds.FeedsSaveMotoSettingsTest.TestAccount
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{CategoryEnum, MotoCategoryEnum, SectionEnum}
import ru.auto.tests.utils.ParameterizedUtils.parameterize
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.utils.Authorization
import ru.auto.tests.publicapi.operations.feeds.RichFeedsCollection
import ru.auto.tests.publicapi.operations.feeds.settings.{GetFeedsSettingsOps, feedSettings}
import ru.auto.tests.publicapi.operations.feeds.settings.moto.{DeleteMotoFeedsSettingsOps, SaveMotoFeedsSettingsOps}

import scala.annotation.meta.getter

@DisplayName("POST /feeds/settings/moto/{moto_category}/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class FeedsSaveMotoSettingsTest(category: MotoCategoryEnum, section: SectionEnum, feedSettingsCheckboxValue: Boolean)
  extends SaveMotoFeedsSettingsOps with DeleteMotoFeedsSettingsOps with GetFeedsSettingsOps with Authorization {

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

    deleteMotoSettings(category, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val settings =
      feedSettings(s"feed_${category.name.toLowerCase}_${section.name.toLowerCase}.xml")
        .isActive(feedSettingsCheckboxValue)
        .deleteSale(feedSettingsCheckboxValue)
        .leaveServices(feedSettingsCheckboxValue)
        .maxDiscountEnabled(feedSettingsCheckboxValue)
        .leaveAddedImages(feedSettingsCheckboxValue)

    saveMotoSettings(settings, category, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val allActualSettings = getSettings()
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val actualSettings = allActualSettings.filterSettings { feed =>
      feed.getCategory == CategoryEnum.MOTO && feed.getMotoCategory == category && feed.getSection == section
    }

    MatcherAssert.assertThat(actualSettings, Matchers.equalTo(List(settings)))
  }

}

object FeedsSaveMotoSettingsTest {

  private val TestAccount = Account.builder()
    .login("FeedsSaveMotoSettingsTest@regress.apiauto.ru")
    .password("autoru")
    .id("70518584")
    .build()

  @Parameterized.Parameters(name = "category = {0}, section = {1}, feedSettingsCheckboxValue = {2}")
  def parameters = parameterize {
    for {
      category <- MotoCategoryEnum.values()
      section <- SectionEnum.values()
      feedSettingsCheckboxValue <- Seq(true, false)
    } yield (category, section, feedSettingsCheckboxValue)
  }

}
