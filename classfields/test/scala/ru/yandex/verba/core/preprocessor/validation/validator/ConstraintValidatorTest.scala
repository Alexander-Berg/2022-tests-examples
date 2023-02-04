package ru.yandex.verba.core.preprocessor.validation.validator

import org.json4s.jackson.JsonMethods._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.application._
import ru.yandex.verba.core.attributes.{Alias, Aliases}
import ru.yandex.verba.core.model.domain.{Languages, Term}
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.preprocessor.validation._
import ru.yandex.verba.core.util.VerbaUtils

import scala.concurrent.duration._

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 08.05.14
  */
class ConstraintValidatorTest extends AnyFlatSpec with Matchers with VerbaUtils {
  DBInitializer

  def print(term: Term) = {
    println(pretty(render(term.entity.asJson)))
    println(
      term.entity.attributes.toMap
        .foldLeft(new StringBuilder()) { (sb, x) =>
          sb.append(x._1).append(" ").append(x._2).append("\n")
        }
        .toString()
    )
  }

  def withAlias(term: Term, attrName: String, value: String) = {
    val aliases: Aliases = Aliases(Map(Languages.Ru -> value), Set.empty)
    Term(term.entity + (attrName -> aliases), term.meta)
  }
  val domain = new Domain
  implicit val timeout: FiniteDuration = 1.minute
  val ref = new OnSaveTransformers()

  "Term mark from db " should " satisfy validator " ignore {
    val validator = new ConstraintValidator("mark-name", "NOT_INTERSECTS", "CHILDREN", "model-name", "model")
    val term = domain.termManager.getFullTerm(Path("/auto/marks/FIAT")).await
    validator.validate(term).isEmpty shouldEqual true
  }

  "Invalid mark term " should " be found by validator " ignore {
    val validator = new ConstraintValidator("mark-name", "NOT_INTERSECTS", "CHILDREN", "model-name", "model")
    var term = domain.termManager.getFullTerm(Path("/auto/marks/FIAT")).await
    term = withAlias(term, "mark-name", "500")
    validator.validate(term).size shouldEqual 1
  }

  "Term generation-name from db " should " satisfy validator " in {
    val validator = ref.getSingleAttrTransformer(
      "CONSTRAINTS [generation-name] [NOT_INTERSECTS] [PARENT] [model-name] [model]",
      "generation-name",
      Path("/auto")
    )
    val term = domain.termManager.getFullTerm(Path("/auto/marks/FORD/models/FOCUS/super-gen/2306579")).await
    val TransformResult(_, _, Seq()) = validator.transform(term)
  }

  "Invalid generation-name  term " should " be found by validator " in {
    val validator = ref.getSingleAttrTransformer(
      "CONSTRAINTS [generation-name] [NOT_INTERSECTS] [PARENT] [model-name] [model]",
      "",
      Path("/auto")
    )
    var term = domain.termManager.getFullTerm(Path("/auto/marks/FORD/models/FOCUS/super-gen/2306579")).await
    term = withAlias(term, "generation-name", "Focus")
    val TransformResult(_, _, Seq(xs)) = validator.transform(term)

  }

  "Invalid generation-name  term " should " be found by validator (alias matching)" in {
    val validator = ref.getSingleAttrTransformer(
      "CONSTRAINTS [generation-name] [NOT_INTERSECTS] [PARENT] [model-name] [model]",
      "",
      Path("/auto")
    )
    var term = domain.termManager.getFullTerm(Path("/auto/marks/FORD/models/FOCUS/super-gen/2306579")).await
    val aliases = Aliases(Map(), Set(Alias("fOkus se", Set(Languages.Ru), 0), Alias("Focus", Set(Languages.Ru), 0)))
    term = Term(term.entity + ("generation-name" -> aliases), term.meta)
    val TransformResult(_, _, Seq(xs)) = validator.transform(term)
  }
}
