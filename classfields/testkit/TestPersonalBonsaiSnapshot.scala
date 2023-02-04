package ru.yandex.vertis.general.personal.testkit

import general.bonsai.category_model.Category
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.personal.logic.PersonalBonsaiSnapshot

object TestPersonalBonsaiSnapshot {

  val rootCategory1: Category = Category(id = "root_category1", parentId = "", order = 1, name = "Электричество")
  val rootCategory2: Category = Category(id = "root_category2", parentId = "", order = 2, name = "Коренья 1")
  val rootCategory3: Category = Category(id = "root_category3", parentId = "", order = 3, name = "Платформа Лось")
  val rootCategory4: Category = Category(id = "root_category4", parentId = "", order = 4, name = "Адронный коллайдер")

  val rootCategory5: Category =
    Category(id = "root_category5", parentId = "", order = 5, name = "Туры по Золотому Кольцу")

  val leafCategory1: Category = Category(id = "leaf_category1", parentId = rootCategory1.id)
  val leafCategory2: Category = Category(id = "leaf_category2", parentId = rootCategory1.id)
  val leafCategory3: Category = Category(id = "leaf_category3", parentId = rootCategory2.id)
  val leafCategory4: Category = Category(id = "leaf_category4", parentId = rootCategory2.id, notForHomepage = true)

  val categories = Seq(
    rootCategory1,
    rootCategory2,
    rootCategory3,
    rootCategory4,
    rootCategory5,
    leafCategory1,
    leafCategory2,
    leafCategory3,
    leafCategory4
  )

  val rootCategories = List(rootCategory1, rootCategory2, rootCategory3, rootCategory4, rootCategory5)

  private val bonsaiSnapshot = BonsaiSnapshot(categories = categories, attributes = Seq())

  val testSnapshot: PersonalBonsaiSnapshot = PersonalBonsaiSnapshot(bonsaiSnapshot)

}
