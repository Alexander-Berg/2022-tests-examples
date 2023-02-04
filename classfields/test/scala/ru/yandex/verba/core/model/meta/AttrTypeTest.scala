package ru.yandex.verba.core.model.meta

import org.json4s.jackson.JsonMethods
import org.json4s.JValue
import org.scalatest.freespec.AnyFreeSpec
import ru.yandex.verba.core.model.domain.Languages
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.util.{JsonUtils, VerbaUtils}

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 08.10.14
  */
class AttrTypeTest extends AnyFreeSpec with VerbaUtils {
  "meta parse" - {
    lazy val json = JsonMethods.parse(getClass.getResourceAsStream("/meta2.json"))
    import JsonUtils.formats

    "print string attr type" in {
      println(StringAttrType("id").prettyRender)
    }
    "print" in {
      val fields = Map("id" -> StringAttrType("id"))
      val dm = DictionaryMeta(
        "name",
        Path("/meta/auto/marks"),
        "${id}",
        "id",
        Seq.empty,
        Seq.empty,
        fields = fields,
        version = -1
      )
      println(JsonUtils.prettyRender(dm.asEntity.asJson))
      println(dm.prettyRender)
      println()
    }

    "parse" in {
      val fields = Map("id" -> StringAttrType("id"))
      val dm = DictionaryMeta(
        "name",
        Path("/meta/auto/marks"),
        "${id}",
        "id",
        Seq.empty,
        Seq.empty,
        fields = fields,
        version = -1
      )
      val json1: JValue = dm.toMyJson
      println(json1)
      println(json1.extract[DictionaryMeta])
      println()
    }

    "parse 2" in {
      val dm = (json \ "fields").extract[Map[String, AttrType]]
      println(dm.prettyRender)
    }

    "path parse" in {
      val json = JsonMethods.parse(""" {"path" : "/meta/auto"} """) \ "path"
      val extractedPath = json.extract[Path]
      val path = Path("/meta/auto")
      assert(extractedPath == path)
    }

    "parse AttrType" in {
      val json = """{"data":{"helpers":{},"multiple":false,"name":"Code","onLoadTransformers":[],"props":{},"translations":{},"type":"string","validators":["REQUIRED"]},"name":"code","type":"string"}"""
      val res = AttrType.fromJson(json)
      assert(res._1 === "code")
      assert(res._2.isInstanceOf[StringAttrType])
    }

    "translations print" in {
      val translations = Map(Languages.Ru.toString -> "dsfsd")
      val json = translations.toMyJson
      val transl = json.extract[Map[String, String]]
      assert(transl.equals(translations))
    }
  }
}
