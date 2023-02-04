package ru.yandex.verba.core.attributes

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.attributes.protocol.AttributesProtocol._
import ru.yandex.verba.core.model.Entity
import ru.yandex.verba.core.model.domain.ImageId
import ru.yandex.verba.core.model.domain.Languages._
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.util.storage.AvaImage
import spray.json._

/**
  * TODO
  */
class AttributesProtocolTest extends AnyFreeSpec with Matchers {
  "Attributes should be" - {
    "serializable and deserializable" in {
      val a = attrs.toJson.convertTo[Attributes].toJson
      val b = attrs.toJson
      println(attrs.toJson.prettyPrint)
      a.prettyPrint shouldEqual b.prettyPrint
    }
  }

  val attrs = Attributes(
    "empty" -> Empty,
    "bool" -> Bool(true),
    "number" -> Number(123.4),
    "test1" -> Str("123"),
    "test2" -> Strings(Seq("a", "b", "c")),
    "ts-left" -> TriState(Left(false)),
    "ts-right" -> TriState(Right("abc")),
    "img" -> Image(ImageId(12351, Some(AvaImage("1111_1111111")))),
    "lnk" -> Link(Entity("a", "A", Path("/a"))),
    "lnks" -> Links(
      Seq(
        Entity("a", "A", Path("/a")),
        Entity("b", "B", Path("/b")),
        Entity("c", "C", Path("/c"))
      )
    ),
    "link-map" -> LinkMap(
      Seq(
        EntityWithParams(Entity("a", "A", Path("/a")), Map.empty),
        EntityWithParams(Entity("b", "B", Path("/b")), Map("price" -> "100500")),
        EntityWithParams(Entity("c", "C", Path("/c")), Map("a" -> "b"))
      )
    ),
    "aliases" -> Aliases(
      Map(
        Ru -> "main-ru"
      ),
      Set(
        Alias("a1", Set(Ru, Tr), 0),
        Alias("a2", Set.empty, 0)
      )
    )
  )
}
