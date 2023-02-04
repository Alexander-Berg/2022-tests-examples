package ru.auto.tests.publicapi.operations.feeds.settings.moto

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.DeleteSettingsMotoOper
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{MotoCategoryEnum, SectionEnum}
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait DeleteMotoFeedsSettingsOps {

  def api: ApiClient

  def deleteMotoSettings(category: MotoCategoryEnum, section: SectionEnum): DeleteSettingsMotoOper = {
    deleteMotoSettings(category.name, section.name)
  }

  def deleteMotoSettingsWithoutAuth(category: MotoCategoryEnum, section: SectionEnum): DeleteSettingsMotoOper = {
    deleteMotoSettingsOper(category.name, section.name)
  }

  def deleteMotoSettings(category: String, section: String): DeleteSettingsMotoOper = {
    deleteMotoSettingsOper(category, section).reqSpec(defaultSpec)
  }

  @Step("Удаляем настройки фида для категории {category} и секции {section}")
  private def deleteMotoSettingsOper(category: String, section: String): DeleteSettingsMotoOper = {
    api.feeds.deleteSettingsMoto()
      .reqSpec(defaultSpec)
      .motoCategoryPath(category)
      .sectionPath(section)
  }

}
