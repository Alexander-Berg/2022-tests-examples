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
import ru.auto.tests.publicapi.feeds.FeedsDeleteMotoSettingsTest.TestAccount
import ru.auto.tests.publicapi.operations.feeds.RichFeedsCollection
import ru.auto.tests.publicapi.operations.feeds.settings.moto.DeleteMotoFeedsSettingsOps
import ru.auto.tests.publicapi.operations.feeds.settings.feedSettings
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{CategoryEnum, MotoCategoryEnum, SectionEnum, TruckCategoryEnum}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.utils.Authorization
import ru.auto.tests.utils.AssertUtils.assertInAnyOrder
import ru.auto.tests.utils.ParameterizedUtils.parameterize

import scala.util.Random
import scala.annotation.meta.getter

@DisplayName("DELETE /feeds/settings/moto/{moto_category}/{section}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class FeedsDeleteMotoSettingsTest(category: MotoCategoryEnum, section: SectionEnum, otherCategory: MotoCategoryEnum, otherSection: SectionEnum, carsCategory: CategoryEnum, trucksCategory: TruckCategoryEnum, otherCategorySection: SectionEnum)
  extends DeleteMotoFeedsSettingsOps with Authorization {

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

    val testMotoCategorySectionSettings = feedSettings(feedName(category.name, section.name))
    feedsAdaptor.saveMotoSettings(testMotoCategorySectionSettings, category, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val otherMotoCategorySettings = feedSettings(feedName(otherCategory.name, section.name))
    feedsAdaptor.saveMotoSettings(otherMotoCategorySettings, otherCategory, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val otherMotoSectionSettings = feedSettings(feedName(category.name, otherSection.name))
    feedsAdaptor.saveMotoSettings(otherMotoSectionSettings, category, otherSection)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val carsSettings = feedSettings(feedName(carsCategory.name, otherCategorySection.name))
    feedsAdaptor.saveCarsSettings(carsSettings, otherCategorySection)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val trucksSettings = feedSettings(feedName(trucksCategory.name, otherCategorySection.name))
    feedsAdaptor.saveTrucksSettings(trucksSettings, trucksCategory, otherCategorySection)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    deleteMotoSettings(category, section)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val allSettingsAfterDelete = feedsAdaptor.getSettings()
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertInAnyOrder(allSettingsAfterDelete.getFeedsList.map(_.getSettings)) {
      List(
        otherMotoCategorySettings,
        otherMotoSectionSettings,
        carsSettings,
        trucksSettings
      )
    }

    val testCategorySectionSettingsAfterDelete = allSettingsAfterDelete.filterSettings { feed =>
      feed.getCategory == CategoryEnum.MOTO && feed.getMotoCategory == category && feed.getSection == section
    }

    MatcherAssert.assertThat(testCategorySectionSettingsAfterDelete.size, Matchers.equalTo(0))
  }

}

object FeedsDeleteMotoSettingsTest {

  private val TestAccount = Account.builder()
    .login("FeedsDeleteMotoSettingsTest@regress.apiauto.ru")
    .password("autoru")
    .id("70519764")
    .build()

  @Parameterized.Parameters(name = "category = {0}, section = {1} (cars = {4}, trucks = {5}, section = {6})")
  def parameters = parameterize {
    for {
      category <- MotoCategoryEnum.values()
      section <- SectionEnum.values()

      otherMotoCategory <- Random.shuffle(MotoCategoryEnum.values().filterNot(_ == category).toList).take(1)
      otherMotoSection <- SectionEnum.values().filterNot(_ == section)

      carsCategory <- Seq(CategoryEnum.CARS)
      trucksCategory <- Random.shuffle(TruckCategoryEnum.values().toList).take(1)
      otherCategorySection <- SectionEnum.values()
    } yield (category, section, otherMotoCategory, otherMotoSection, carsCategory, trucksCategory, otherCategorySection)
  }
}


