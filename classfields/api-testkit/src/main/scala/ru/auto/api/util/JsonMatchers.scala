package ru.auto.api.util

import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.libs.json._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 28.02.17
  */
trait JsonMatchers {

  //scalastyle:off
  private def matches(js: JsValue, expected: JsValue, path: Seq[String]): Option[String] = {
    val pathStr = path.mkString(".", ".", "")
    (js, expected) match {
      case (JsString(left), JsString(right)) =>
        if (left != right)
          Some(s"At $pathStr expected string [$right], got [$left]")
        else None
      case (JsNumber(left), JsNumber(right)) =>
        if (left != right)
          Some(s"At $pathStr expected number [$right], got [$left]")
        else None
      case (JsBoolean(left), JsBoolean(right)) =>
        if (left != right) Some(s"At $pathStr expected [$right], got [$left]")
        else None
      case (JsArray(left), JsArray(right)) =>
        if (left != right)
          Some(
            s"At $pathStr\n" +
              s"Expected: [${right.mkString(", ")}]\n" +
              s"Actual:   [${left.mkString(", ")}]"
          )
        else None
      case (JsObject(left), JsObject(right)) =>
        val it = right.iterator
          .flatMap {
            case (name, r) =>
              def pathStr = (path :+ name).mkString(".", ".", "")
              left.get(name) match {
                case Some(l) => matches(l, r, path :+ name)
                case None => Some(s"At $pathStr expected [$r], got nothing")
              }
          }
        if (it.hasNext) Some(it.next()) else None
      case _ =>
        Some(s"At $pathStr expected $expected, got $js")
    }
  }

  def matchJson(expected: JsValue): Matcher[JsValue] = {
    Matcher[JsValue](obj => {
      val matchingError = matches(obj, expected, Seq.empty)
      MatchResult(
        matchingError.isEmpty,
        matchingError.getOrElse("") + "\nFull json:\n" + Json.prettyPrint(obj),
        s"Not expected"
      )
    })
  }

  def matchJson(expected: String): Matcher[String] = {
    Matcher[String](json => {
      val obj = Json.parse(json)
      val expectedJson = Json.parse(expected)
      val matchingError = matches(obj, expectedJson, Seq.empty)
      MatchResult(
        matchingError.isEmpty,
        matchingError.getOrElse("") + "\nFull json:\n" + Json.prettyPrint(obj),
        s"Not expected"
      )
    })
  }
}

object JsonMatchers extends JsonMatchers
