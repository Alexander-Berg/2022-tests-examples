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
import ru.auto.tests.publicapi.adaptor.{PublicApiAdaptor, PublicApiDealerFeedsAdaptor}
import ru.auto.tests.publicapi.consts.Owners.DEALER_PRODUCTS
import ru.auto.tests.publicapi.feeds.FeedsDeleteCarsSettingsTest.TestAccount
import ru.auto.tests.publicapi.operations.feeds.RichFeedsCollection
import ru.auto.tests.publicapi.operations.feeds.settings.cars.DeleteCarsFeedsSettingsOps
import ru.auto.tests.publicapi.operations.feeds.settings.feedSettings
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{CategoryEnum, MotoCategoryEnum, SectionEnum, TruckCategoryEnum}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.utils.Authorization
import ru.auto.tests.utils.AssertUtils.assertInAnyOrder
import ru.auto.tests.utils.ParameterizedUtils.parameterize

import scala.util.Random
import scala.annotation.meta.getter

@DisplayName("DELETE /feeds/settings/cars/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class FeedsDeleteCarsSettingsTest(section: SectionEnum, otherSection: SectionEnum, motoCategory: MotoCategoryEnum, trucksCategory: TruckCategoryEnum, otherCategorySection: SectionEnum)
  extends DeleteCarsFeedsSettingsOps with Authorization {

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

    feedsAdaptor.clearSettings(sessionId)

    def feedName(category: String, section: String): String = {
      s"feed_${category}_${section}.xml"
    }

    val testSectionSettings = feedSettings(feedName("cars", section.name))
    feedsAdaptor.saveCarsSettings(testSectionSettings, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val otherSectionSettings = feedSettings(feedName("cars", otherSection.name))
    feedsAdaptor.saveCarsSettings(otherSectionSettings, otherSection)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val motoSettings = feedSettings(feedName(motoCategory.name, otherCategorySection.name))
    feedsAdaptor.saveMotoSettings(motoSettings, motoCategory, otherCategorySection)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val trucksSettings = feedSettings(feedName(trucksCategory.name, otherCategorySection.name))
    feedsAdaptor.saveTrucksSettings(trucksSettings, trucksCategory, otherCategorySection)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    deleteCarsSettings(section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val allSettingsAfterDelete = feedsAdaptor.getSettings()
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertInAnyOrder(allSettingsAfterDelete.getFeedsList.map(_.getSettings)) {
      List(
        otherSectionSettings,
        motoSettings,
        trucksSettings
      )
    }

    val testSectionSettingsAfterDelete = allSettingsAfterDelete.filterSettings { feed =>
      feed.getCategory == CategoryEnum.CARS && feed.getSection == section
    }

    MatcherAssert.assertThat(testSectionSettingsAfterDelete.size, Matchers.equalTo(0))
  }

}

object FeedsDeleteCarsSettingsTest {

  private val TestAccount = Account.builder()
    .login("FeedsDeleteCarsSettingsTest@regress.apiauto.ru")
    .password("autoru")
    .id("70519765")
    .build()

  @Parameterized.Parameters(name = "section = {0} (moto = {2}, trucks = {3}, section = {4})")
  def parameters = parameterize {
    for {
      section <- SectionEnum.values()
      otherCarsSection <- SectionEnum.values().filterNot(_ == section)
      motoCategory <- Random.shuffle(MotoCategoryEnum.values().toList).take(1)
      trucksCategory <- Random.shuffle(TruckCategoryEnum.values().toList).take(1)
      otherCategorySection <- SectionEnum.values()
    } yield (section, otherCarsSection, motoCategory, trucksCategory, otherCategorySection)
  }
}


