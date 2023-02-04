package ru.auto.tests.publicapi.operations.feeds.tasks.trucks

import io.qameta.allure.Step
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.FeedsApi.CreateTaskTrucksOper
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedFeedTaskTaskSettings
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedCategorySection.{TruckCategoryEnum, SectionEnum}
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait CreateTrucksFeedsTaskOps {

  def api: ApiClient

  def createTrucksTask(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, category: TruckCategoryEnum, section: SectionEnum): CreateTaskTrucksOper = {
    createTrucksTask(settings, category.name, section.name)
  }

  def createTrucksTaskWithoutAuth(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, category: TruckCategoryEnum, section: SectionEnum): CreateTaskTrucksOper = {
    createTrucksTaskOper(settings, category.name, section.name)
  }

  def createTrucksTask(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, category: String, section: String): CreateTaskTrucksOper = {
    createTrucksTaskOper(settings, category, section).reqSpec(defaultSpec)
  }

  @Step("Добавляем фид на ручкую загрузку для категории {category} и секции {section}: {settings.internalUrl} / {settings.settings.source}")
  private def createTrucksTaskOper(settings: AutoApiFeedprocessorFeedFeedTaskTaskSettings, category: String, section: String): CreateTaskTrucksOper = {
    api.feeds.createTaskTrucks()
      .trucksCategoryPath(category)
      .sectionPath(section)
      .body(settings)
  }

}
