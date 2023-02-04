package ru.yandex.auto.searchline.manager

import org.scalatest.matchers._
import ru.yandex.auto.searchline.model.Suggest

/**
  * @author pnaydenov
  */
trait SuggestMatchers {
  class MarkModelMatcher(expected: String) extends Matcher[Suggest] {
    override def apply(left: Suggest): MatchResult = {
      val current = if (left.params.getMarkModelNameplateCount > 0) left.params.getMarkModelNameplate(0) else ""
      val matches = current.equals(expected)
      val currentLabel = if (current.isEmpty) "<empty>" else current
      val expectedLabel = if (expected.isEmpty) "<empty>" else expected

      MatchResult(matches,
        s"suggest with $currentLabel don't match $expectedLabel",
        s"suggest with $currentLabel match $expectedLabel")
    }
  }

  def matchMarkModel(mark: String,
                     model: String = "",
                     generation: String = "",
                     nameplate: String = ""): MarkModelMatcher = {
    val expected = List(mark, model, nameplate, generation).reverse.dropWhile(_.isEmpty).reverse.mkString("#")
    new MarkModelMatcher(expected)
  }
}

object SuggestMatchers extends SuggestMatchers
