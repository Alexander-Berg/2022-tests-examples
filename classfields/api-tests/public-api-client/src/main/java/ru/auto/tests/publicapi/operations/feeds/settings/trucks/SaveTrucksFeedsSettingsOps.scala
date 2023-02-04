package ru.auto.tests.publicapi.operations.feeds.settings.trucks

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.SaveSettingsTrucksOper
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{TruckCategoryEnum, SectionEnum}
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedFeedSettings
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait SaveTrucksFeedsSettingsOps {

  def api: ApiClient

  def saveTrucksSettingsWithoutAuth(settings: AutoApiFeedprocessorFeedFeedSettings, category: TruckCategoryEnum, section: SectionEnum): SaveSettingsTrucksOper = {
    saveTrucksSettingsOper(settings, category.name, section.name)
  }

  def saveTrucksSettings(settings: AutoApiFeedprocessorFeedFeedSettings, category: TruckCategoryEnum, section: SectionEnum): SaveSettingsTrucksOper = {
    saveTrucksSettings(settings, category.name, section.name)
  }

  def saveTrucksSettings(settings: AutoApiFeedprocessorFeedFeedSettings, category: String, section: String): SaveSettingsTrucksOper = {
    saveTrucksSettingsOper(settings, category, section)
      .reqSpec(defaultSpec)
  }

  @Step("Сохраняем настройки фида для категории {category} и секции {section}: {settings.source}")
  private def saveTrucksSettingsOper(settings: AutoApiFeedprocessorFeedFeedSettings, category: String, section: String): SaveSettingsTrucksOper = {
    api.feeds.saveSettingsTrucks()
      .trucksCategoryPath(category)
      .sectionPath(section)
      .body(settings)
  }

}
