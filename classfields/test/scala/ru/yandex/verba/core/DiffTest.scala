package ru.yandex.verba.core

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.attributes._
import ru.yandex.verba.core.attributes.diff._
import ru.yandex.verba.core.model.domain.{ImageId, Languages}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 11.11.13 17:50
  */
class DiffTest extends AnyFreeSpec with Matchers {

  "Diff" - {
    "should build diff from two set of attributes" in {
      val from = Attributes(
        "a" -> Str("A"),
        "b" -> Strings(Seq("b1", "b2")),
        "d" -> Bool(true),
        "h" -> Aliases(Map.empty, Set(Alias("test", Set(Languages.En), 0))),
        "s" -> Image(ImageId(500, None))
      )

      val to = Attributes(
        "a" -> Str("AA"),
        "c" -> TriState(Left(true)),
        "d" -> Bool(false),
        "h" -> Aliases(Map.empty, Set(Alias("test", Set(Languages.Ru), 0))),
        "s" -> Image(ImageId(500, None))
      )

      val diff: Diff = Diff(from, to)
      diff shouldEqual Diff(
        "a" -> UpdatedAttribute(Str("A"), Str("AA")),
        "b" -> MultipleChange(2, 0),
        "c" -> AddedAttribute(TriState(Left(true))),
        "h" -> AliasesDiff(0, Seq(UpdatedAliasLang("test", Set(Languages.En), Set(Languages.Ru)))),
        "d" -> UpdatedAttribute(Bool(true), Bool(false)),
        "s" -> NotChanged
      )
      println(diff)
    }
  }
}
