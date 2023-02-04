package ru.auto.tests.publicapi.operations

import ru.auto.tests.publicapi.model.{AutoApiFeedprocessorFeed, AutoApiFeedprocessorFeedCategorySection, AutoApiFeedprocessorFeedCollection, AutoApiFeedprocessorFeedFeedSettings}
import scala.jdk.CollectionConverters._

package object feeds {

  implicit class RichFeedsCollection(val settings: AutoApiFeedprocessorFeedCollection) {
    def getFeedsList: List[AutoApiFeedprocessorFeed] = {
      settings.getFeeds.asScala.toList
    }

    def filterSettings(f: AutoApiFeedprocessorFeedCategorySection => Boolean): List[AutoApiFeedprocessorFeedFeedSettings] = {
      getFeedsList
        .filter(feed => f(feed.getCategory))
        .map(_.getSettings)
    }
  }

}
