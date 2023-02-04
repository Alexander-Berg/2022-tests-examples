package ru.auto.tests.publicapi.adaptor

import com.google.inject.Inject
import com.google.inject.Singleton
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.CategoryEnum
import ru.auto.tests.publicapi.operations.feeds.settings.AllFeedsSettingsOps
import ru.auto.tests.publicapi.operations.feeds.RichFeedsCollection
import ru.auto.tests.publicapi.operations.feeds.tasks.GetFeedsHistoryOps

@Singleton
class PublicApiDealerFeedsAdaptor extends PublicApiAdaptor
  with AllFeedsSettingsOps
  with GetFeedsHistoryOps {

  @Inject
  @Prod
  override val api: ApiClient = null

  def clearSettings(sessionId: String): Unit = {
    val currentSettings =
      getSettings()
        .xSessionIdHeader(sessionId)
        .executeAs(validatedWith(shouldBe200OkJSON))
        .getFeedsList

    currentSettings.map(_.getCategory).foreach { feed =>
      feed.getCategory match {
        case CategoryEnum.CARS =>
          deleteCarsSettings(feed.getSection)
            .xSessionIdHeader(sessionId)
            .execute(validatedWith(shouldBe200OkJSON))
        case CategoryEnum.MOTO =>
          deleteMotoSettings(feed.getMotoCategory, feed.getSection)
            .xSessionIdHeader(sessionId)
            .execute(validatedWith(shouldBe200OkJSON))
        case CategoryEnum.TRUCKS =>
          deleteTrucksSettings(feed.getTruckCategory, feed.getSection)
            .xSessionIdHeader(sessionId)
            .execute(validatedWith(shouldBe200OkJSON))
      }
    }
  }

}