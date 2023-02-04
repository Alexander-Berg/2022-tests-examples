package ru.auto.tests.publicapi.operations.feeds.tasks.cars

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.CreateTaskCarsOper
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedFeedTaskTaskSettings
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.SectionEnum
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait CreateCarsFeedsTaskOps {

  def api: ApiClient

  def createCarsTask(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, section: SectionEnum): CreateTaskCarsOper = {
    createCarsTask(settings, section.name)
  }

  def createCarsTaskWithoutAuth(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, section: SectionEnum): CreateTaskCarsOper = {
    createCarsTaskOper(settings, section.name)
  }

  def createCarsTask(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, section: String): CreateTaskCarsOper = {
    createCarsTaskOper(settings, section).reqSpec(defaultSpec)
  }

  @Step("Добавляем фид на ручную загрузку для секции {section}: {settings.internalUrl} / {settings.settings.source}")
  private def createCarsTaskOper(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, section: String): CreateTaskCarsOper = {
    api.feeds.createTaskCars()
      .sectionPath(section)
      .body(settings)
  }

}
