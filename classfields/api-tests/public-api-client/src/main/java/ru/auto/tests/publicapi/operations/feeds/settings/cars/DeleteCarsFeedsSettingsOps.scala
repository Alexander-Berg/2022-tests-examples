package ru.auto.tests.publicapi.operations.feeds.settings.cars

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.DeleteSettingsCarsOper
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.SectionEnum
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait DeleteCarsFeedsSettingsOps {

  def api: ApiClient

  def deleteCarsSettings(section: SectionEnum): DeleteSettingsCarsOper = {
    deleteCarsSettings(section.name)
  }

  def deleteCarsSettingsWithoutAuth(section: SectionEnum): DeleteSettingsCarsOper = {
    deleteCarsSettingsOper(section.name)
  }

  def deleteCarsSettings(section: String): DeleteSettingsCarsOper = {
    deleteCarsSettingsOper(section).reqSpec(defaultSpec)
  }

  @Step("Удаляем настройки фида для секции {section}")
  private def deleteCarsSettingsOper(section: String): DeleteSettingsCarsOper = {
    api.feeds.deleteSettingsCars()
      .sectionPath(section)
  }

}
