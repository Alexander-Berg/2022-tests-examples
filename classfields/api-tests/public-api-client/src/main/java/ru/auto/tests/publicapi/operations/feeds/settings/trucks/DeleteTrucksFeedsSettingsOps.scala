package ru.auto.tests.publicapi.operations.feeds.settings.trucks

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.DeleteSettingsTrucksOper
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{TruckCategoryEnum, SectionEnum}
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait DeleteTrucksFeedsSettingsOps {

  def api: ApiClient

  def deleteTrucksSettings(category: TruckCategoryEnum, section: SectionEnum): DeleteSettingsTrucksOper = {
    deleteTrucksSettings(category.name, section.name)
  }

  def deleteTrucksSettingsWithoutAuth(category: TruckCategoryEnum, section: SectionEnum): DeleteSettingsTrucksOper = {
    deleteTrucksSettingsOper(category.name, section.name)
  }

  def deleteTrucksSettings(category: String, section: String): DeleteSettingsTrucksOper = {
    deleteTrucksSettingsOper(category, section).reqSpec(defaultSpec)
  }

  @Step("Удаляем настройки фида для категории {category} и секции {section}")
  private def deleteTrucksSettingsOper(category: String, section: String): DeleteSettingsTrucksOper = {
    api.feeds.deleteSettingsTrucks()
      .reqSpec(defaultSpec)
      .trucksCategoryPath(category)
      .sectionPath(section)
  }

}
