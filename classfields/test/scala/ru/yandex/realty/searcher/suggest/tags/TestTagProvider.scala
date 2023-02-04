package ru.yandex.realty.searcher.suggest.tags

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.model.tags.{CategoryAndTypeRestriction, Tag}
import ru.yandex.realty.tags.TagsRuntime

/**
  * @author nstaroverova
  */
object TestTagProvider extends Provider[TagsRuntime] {

  val tag1 = Tag(
    1,
    "коммуналка",
    8,
    Seq("коммуналка", "коммунальная квартира"),
    CategoryAndTypeRestriction(Set.empty, Set.empty)
  )

  val tag2 = Tag(
    2,
    "двухуровневые квартиры",
    2,
    Seq("двухуровневые квартиры", "двухуровневая квартира"),
    CategoryAndTypeRestriction(Set.empty, Set.empty)
  )

  val tag3 = Tag(
    3,
    "с панорамными окнами",
    3,
    Seq("с панорамными окнами", "окна в пол", "панорамные окна"),
    CategoryAndTypeRestriction(Set.empty, Set.empty)
  )
  val tag4 = Tag(4, "пентхаус", 7, Seq("пентхаус"), CategoryAndTypeRestriction(Set.empty, Set.empty))

  val tag5 = Tag(
    5,
    "с пластиковыми окнами",
    5,
    Seq("с пластиковыми окнами", "пластиковые окна"),
    CategoryAndTypeRestriction(Set.empty, Set.empty)
  )

  private val tags: Map[Long, Tag] = Set(tag1, tag2, tag3, tag4, tag5).map(t => (t.id, t)).toMap
  val tagRuntime = TagsRuntime(tags, Set.empty)

  override def get(): TagsRuntime = tagRuntime
}
