package ru.auto.tests.publicapi.operations.feeds.settings.cars

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.SaveSettingsCarsOper
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.SectionEnum
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedFeedSettings
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait SaveCarsFeedsSettingsOps {

  def api: ApiClient

  def saveCarsSettings(settings: AutoApiFeedprocessorFeedFeedSettings, section: SectionEnum): SaveSettingsCarsOper = {
    saveCarsSettings(settings, section.name)
  }

  def saveCarsSettingsWithoutAuth(settings: AutoApiFeedprocessorFeedFeedSettings, section: SectionEnum): SaveSettingsCarsOper = {
    saveCarsSettingsOper(settings, section.name)
  }

  def saveCarsSettings(settings: AutoApiFeedprocessorFeedFeedSettings, section: String): SaveSettingsCarsOper = {
    saveCarsSettingsOper(settings, section).reqSpec(defaultSpec)
  }

  @Step("Сохраняем настройки фида для секции {section}: {settings.source}")
  private def saveCarsSettingsOper(settings: AutoApiFeedprocessorFeedFeedSettings, section: String): SaveSettingsCarsOper = {
    api.feeds.saveSettingsCars()
      .sectionPath(section)
      .body(settings)
  }

}
