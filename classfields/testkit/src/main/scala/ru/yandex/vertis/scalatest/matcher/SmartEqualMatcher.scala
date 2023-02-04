package ru.yandex.vertis.scalatest.matcher

import org.scalatest.matchers.{Matcher, MatchResult}

import scala.language.implicitConversions
import scala.util.matching.Regex

/**
  * Smart equal matcher.
  * It prints detailed report about difference between two objects.
  * Example usage:
  * {{{
  *    actual should smartEqual(expected)
  * }}}
  * Note: it doesn't work with null values!
  *
  * @param right right operand (actual value).
  * @param blackList list describes fields that must be ignored while compare.
  *
  * @see [[Differ]]
  *
  * @author semkagtn
  */
class SmartEqualMatcher[-T](right: T, blackList: List[Regex])
  extends Matcher[T] {

  /**
    * Ignore fields that matches specified regex.
    */
  def ignoreFields(regex: Regex): SmartEqualMatcher[T] =
    new SmartEqualMatcher(right, regex :: blackList)

  /**
    * Ignore fields that matches specified regex.
    */
  def ignoreFields(regex: String): SmartEqualMatcher[T] =
    ignoreFields(regex.r)

  override def apply(left: T): MatchResult = {
    val differ = new Differ[T](blackList)

    val diffs = differ.diffs(left, right)
    val matches = diffs.isEmpty
    val rawFailureMessage = diffs.map {
      case Diff.Added(fieldPath, actual) =>
        s"$fieldPath ADDED: $actual"
      case Diff.Removed(fieldPath, expected) =>
        s"$fieldPath REMOVED: $expected"
      case Diff.Changed(fieldPath, actual, expected) =>
        s"$fieldPath CHANGED: $actual != $expected"
    }.mkString("\n")
    val rawNegateFailureMessage = s"$left equaled $right"

    MatchResult(
      matches,
      rawFailureMessage,
      rawNegateFailureMessage)
  }
}

object SmartEqualMatcher {

  /**
    * Factory method for [[SmartEqualMatcher]].
    */
  def smartEqual[T](right: T): SmartEqualMatcher[T] =
    new SmartEqualMatcher[T](right, List.empty)

  implicit def generalizeType[A, B](matcher: SmartEqualMatcher[A])
                                   (implicit ev: A <:< B): SmartEqualMatcher[B] =
    matcher.asInstanceOf[SmartEqualMatcher[B]]
}
