package ru.yandex.verba.core.preprocessor.validation.transformer

import org.scalatest.time.{Millis, Span}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.application._
import ru.yandex.verba.core.attributes.{Aliases, Attributes, Str, Strings}
import ru.yandex.verba.core.model.Entity
import ru.yandex.verba.core.model.domain.{Languages, Term}
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.util.VerbaUtils

import scala.concurrent.duration.FiniteDuration

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 21.07.14
  */
/*
class RestUrlTransformerTest extends AnyFreeSpec with VerbaUtils with Timeouts {
  val attrsValid = Attributes.apply(Map("a" -> Str("abc")))
  val attrsInvalid = Attributes.apply(Map("a" -> Strings(Seq("abc", "ade"))))

  def asTerm(attrs: Attributes) = Term(Entity("", "", Path("/auto/valid"), attributes = attrs), null)
  DBInitializer
  implicit val ec = system.dispatcher
  "Rest transformer" - {
    //rest url transformer shouldn't init "used" during creating. Only in case semantic-url == Empty
    failAfter(Span(1000, Millis)) {
      new RestUrlTransformer()
    }
  }
}
 */
