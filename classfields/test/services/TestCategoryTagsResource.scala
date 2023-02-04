package ru.yandex.vertis.general.wizard.api.services

import ru.yandex.vertis.general.wizard.core.service.CategoryTagsService
import ru.yandex.vertis.general.wizard.model.{CategoryId, CategoryTag}
import zio.Task

case class TestCategoryTagsResource(private val map: Map[CategoryTag, Set[CategoryId]])
  extends CategoryTagsService.Service {

  override def hasTag(categoryId: CategoryId, categoryTag: CategoryTag): Task[Boolean] =
    Task.succeed(
      map.getOrElse(categoryTag, Set.empty).contains(categoryId)
    )
}

object TestCategoryTagsResource {

  def simple(categoryTag: CategoryTag, ids: Set[CategoryId]): CategoryTagsService.Service =
    TestCategoryTagsResource(Map(categoryTag -> ids))

  def empty: CategoryTagsService.Service = TestCategoryTagsResource(Map.empty)
}
