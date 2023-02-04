package ru.auto.tests.publicapi.operations.feeds.tasks.moto

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.CreateTaskMotoOper
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedFeedTaskTaskSettings
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{MotoCategoryEnum, SectionEnum}
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait CreateMotoFeedsTaskOps {

  def api: ApiClient

  def createMotoTask(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, category: MotoCategoryEnum, section: SectionEnum): CreateTaskMotoOper = {
    createMotoTask(settings, category.name, section.name)
  }

  def createMotoTaskWithoutAuth(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, category: MotoCategoryEnum, section: SectionEnum): CreateTaskMotoOper = {
    createMotoTaskOper(settings, category.name, section.name)
  }

  def createMotoTask(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, category: String, section: String): CreateTaskMotoOper = {
    createMotoTaskOper(settings, category, section).reqSpec(defaultSpec)
  }

  @Step("Добавляем фид на ручкую загрузку для категории {category} и секции {section}: {settings.internalUrl} / {settings.settings.source}")
  private def createMotoTaskOper(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, category: String, section: String): CreateTaskMotoOper = {
    api.feeds.createTaskMoto()
      .motoCategoryPath(category)
      .sectionPath(section)
      .body(settings)
  }

}
