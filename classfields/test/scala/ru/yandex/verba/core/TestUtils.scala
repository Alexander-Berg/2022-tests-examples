package ru.yandex.verba.core

import ru.yandex.verba.core.attributes.{Attribute, Attributes}
import ru.yandex.verba.core.model.Entity
import ru.yandex.verba.core.model.domain.Term
import ru.yandex.verba.core.model.meta._
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.preprocessor.validation.TransformResult

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 02.08.14
  */
object TestUtils {

  private def asMeta =
    DictionaryMeta(
      "valid",
      Path("/meta/auto/valid"),
      "",
      "id",
      Seq.empty,
      Seq.empty,
      fields = Map("id" -> StringAttrType("x")),
      version = 1
    )

  def asTerm(attrs: Attributes): Term =
    Term(Entity("", "", Path("/auto/valid"), attributes = attrs), asMeta)

  def asTerm(attr: Attribute): Term =
    asTerm(Attributes(Map("a" -> attr)))

  implicit class RichTransformResult(tr: TransformResult) {
    def shouldValid() = require(tr.warnings.isEmpty && tr.problems.isEmpty, s"Should valid, but ${tr.toString}")
    def shouldWarning() = require(tr.warnings.nonEmpty && tr.problems.isEmpty, s"Should warnings, but ${tr.toString}")
    def shouldProblems() = require(tr.problems.nonEmpty, s"Should problems, but ${tr.toString}")
  }
}
