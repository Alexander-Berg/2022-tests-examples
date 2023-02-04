package ru.auto.tests.publicapi.operations.feeds.settings.moto

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.SaveSettingsMotoOper
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{MotoCategoryEnum, SectionEnum}
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedFeedSettings
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait SaveMotoFeedsSettingsOps {

  def api: ApiClient

  def saveMotoSettingsWithoutAuth(settings: AutoApiFeedprocessorFeedFeedSettings, category: MotoCategoryEnum, section: SectionEnum): SaveSettingsMotoOper = {
    saveMotoSettingsOper(settings, category.name, section.name)
  }

  def saveMotoSettings(settings: AutoApiFeedprocessorFeedFeedSettings, category: MotoCategoryEnum, section: SectionEnum): SaveSettingsMotoOper = {
    saveMotoSettings(settings, category.name, section.name)
  }

  def saveMotoSettings(settings: AutoApiFeedprocessorFeedFeedSettings, category: String, section: String): SaveSettingsMotoOper = {
    saveMotoSettingsOper(settings, category, section)
      .reqSpec(defaultSpec)
  }

  @Step("Сохраняем настройки фида для категории {category} и секции {section}: {settings.source}")
  private def saveMotoSettingsOper(settings: AutoApiFeedprocessorFeedFeedSettings, category: String, section: String): SaveSettingsMotoOper = {
    api.feeds.saveSettingsMoto()
      .motoCategoryPath(category)
      .sectionPath(section)
      .body(settings)
  }

}
