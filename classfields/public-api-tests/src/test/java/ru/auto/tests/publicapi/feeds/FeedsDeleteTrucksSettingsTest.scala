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
import ru.auto.tests.publicapi.feeds.FeedsDeleteTrucksSettingsTest.TestAccount
import ru.auto.tests.publicapi.operations.feeds.RichFeedsCollection
import ru.auto.tests.publicapi.operations.feeds.settings.trucks.DeleteTrucksFeedsSettingsOps
import ru.auto.tests.publicapi.operations.feeds.settings.feedSettings
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{CategoryEnum, MotoCategoryEnum, SectionEnum, TruckCategoryEnum}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.utils.Authorization
import ru.auto.tests.utils.AssertUtils.assertInAnyOrder
import ru.auto.tests.utils.ParameterizedUtils.parameterize

import scala.util.Random
import scala.annotation.meta.getter

@DisplayName("DELETE /feeds/settings/trucks/{trucks_category}/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class FeedsDeleteTrucksSettingsTest(category: TruckCategoryEnum, section: SectionEnum, otherCategory: TruckCategoryEnum, otherSection: SectionEnum, carsCategory: CategoryEnum, motoCategory: MotoCategoryEnum, otherCategorySection: SectionEnum)
  extends DeleteTrucksFeedsSettingsOps with Authorization {

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

    val testTrucksCategorySectionSettings = feedSettings(feedName(category.name, section.name))
    feedsAdaptor.saveTrucksSettings(testTrucksCategorySectionSettings, category, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val otherTrucksCategorySettings = feedSettings(feedName(otherCategory.name, section.name))
    feedsAdaptor.saveTrucksSettings(otherTrucksCategorySettings, otherCategory, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val otherTrucksSectionSettings = feedSettings(feedName(category.name, otherSection.name))
    feedsAdaptor.saveTrucksSettings(otherTrucksSectionSettings, category, otherSection)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val carsSettings = feedSettings(feedName(carsCategory.name, otherCategorySection.name))
    feedsAdaptor.saveCarsSettings(carsSettings, otherCategorySection)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val motoSettings = feedSettings(feedName(motoCategory.name, otherCategorySection.name))
    feedsAdaptor.saveMotoSettings(motoSettings, motoCategory, otherCategorySection)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    deleteTrucksSettings(category, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val allSettingsAfterDelete = feedsAdaptor.getSettings()
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertInAnyOrder(allSettingsAfterDelete.getFeedsList.map(_.getSettings)) {
      List(
        otherTrucksCategorySettings,
        otherTrucksSectionSettings,
        carsSettings,
        motoSettings
      )
    }

    val testCategorySectionSettingsAfterDelete = allSettingsAfterDelete.filterSettings { feed =>
      feed.getCategory == CategoryEnum.TRUCKS && feed.getTruckCategory == category && feed.getSection == section
    }

    MatcherAssert.assertThat(testCategorySectionSettingsAfterDelete.size, Matchers.equalTo(0))
  }

}

object FeedsDeleteTrucksSettingsTest {

  private val TestAccount = Account.builder()
    .login("FeedsDeleteTrucksSettingsTest@regress.apiauto.ru")
    .password("autoru")
    .id("70519763")
    .build()

  @Parameterized.Parameters(name = "category = {0}, section = {1} (cars = {4}, trucks = {5}, section = {6})")
  def parameters = parameterize {
    for {
      category <- TruckCategoryEnum.values()
      section <- SectionEnum.values()

      otherTrucksCategory <- Random.shuffle(TruckCategoryEnum.values().filterNot(_ == category).toList).take(1)
      otherTrucksSection <- SectionEnum.values().filterNot(_ == section)

      carsCategory <- Seq(CategoryEnum.CARS)
      motoCategory <- Random.shuffle(MotoCategoryEnum.values().toList).take(1)
      otherCategorySection <- SectionEnum.values()
    } yield (category, section, otherTrucksCategory, otherTrucksSection, carsCategory, motoCategory, otherCategorySection)
  }
}


