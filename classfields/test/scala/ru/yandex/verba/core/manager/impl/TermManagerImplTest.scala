package ru.yandex.verba.core.manager.impl

import org.json4s.jackson.JsonMethods
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import ru.yandex.verba.core.application.DBInitializer
import ru.yandex.verba.core.manager.TermManager
import ru.yandex.verba.core.model.meta.DictionaryMeta
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.util.{JsonUtils, VerbaUtils}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * User: Vladislav Dolbilov (darl@yandex-team.ru)
  * Date: 14.05.13 16:54
  */
class TermManagerImplTest extends AnyFreeSpec with VerbaUtils with OptionValues {
  DBInitializer
  val termManager = TermManager.ref

  implicit val ec = ExecutionContext.global
  implicit val timeout = Duration("30 s")

  "meta parse" - {
    val json = JsonMethods.parse(getClass.getResourceAsStream("/meta.json"))

    "json should be parseable with DictionaryMeta" ignore {
      import JsonUtils._
      val metaJson = json \ "meta"
      val dm = metaJson.extract[DictionaryMeta]
      val newMetaJson = dm.toMyJson
      println(JsonUtils.prettyRender(newMetaJson))
    }
    "term parse" ignore {
      val term = termManager.parseTerm(json, parseMeta = true).await
      println(term)
    }
    "meta write" ignore {
      val term = termManager.getFullTerm(Path("/auto/marks/BMW")).await
      println(JsonUtils.prettyRender(term.asJson))
    }

  }
}
