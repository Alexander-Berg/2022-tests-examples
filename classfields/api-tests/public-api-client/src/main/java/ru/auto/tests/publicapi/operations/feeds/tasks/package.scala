package ru.auto.tests.publicapi.operations.feeds

import ru.auto.tests.publicapi.model.{AutoApiFeedprocessorFeedFeedSettings, AutoApiFeedprocessorFeedFeedTaskTaskSettings}
import ru.auto.tests.publicapi.operations.feeds.settings.defaultFeedSettings

package object tasks {

  def taskSettings(internalUrl: String, settings: AutoApiFeedprocessorFeedFeedSettings): AutoApiFeedprocessorFeedFeedTaskTaskSettings = {
    new AutoApiFeedprocessorFeedFeedTaskTaskSettings()
      .internalUrl(internalUrl)
      .settings(settings)
  }

  val defaultTaskSettings: AutoApiFeedprocessorFeedFeedTaskTaskSettings =
    taskSettings("internal_feed.xml", defaultFeedSettings)

}
