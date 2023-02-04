package ru.yandex.vertis.scalatest.matcher

import scala.reflect.runtime.currentMirror
import scala.util.matching.Regex

/**
  * Finds detailed difference between two object of same type.
  * Note: it doesn't work with null values!
  *
  * @param blackList fields to ignore while calculate difference.
  *
  * @see [[SmartEqualMatcher]]
  *
  * @author semkagtn
  */
class Differ[-T](blackList: Iterable[Regex]) {

  import Differ._

  def diffs(actual: T, expected: T): Seq[Diff] = {
    if (actual == null && expected == null) {
      Seq.empty
    } else if (actual == null || expected == null) {
      Seq(Diff.Changed("/", actual, expected))
    } else {
      val actualMap = toMap(actual)
      val expectedMap = toMap(expected)
      val diffs = getDiffs(actualMap, expectedMap)
      diffs.toSeq.sortBy(_.fieldPath)
    }
  }

  private def inBlackList(path: FieldPath): Boolean =
    blackList.exists { regex =>
      regex.findFirstMatchIn(path.toString).isDefined
    }

  private def squashPaths(paths: Set[FieldPath]): Set[FieldPath] =
    paths.foldLeft(Set.empty[FieldPath]) { (acc, path) =>
      if (!paths.exists(path.isAncestorOf))
        acc + path
      else
        acc
    }

  private def getDiffs(actual: Map[FieldPath, Any],
                       expected: Map[FieldPath, Any]): Iterable[Diff] = {
    val keys = squashPaths(actual.keySet | expected.keySet).filterNot(inBlackList)
    val result: Set[Option[Diff]] = for (key <- keys) yield {
      (actual.get(key), expected.get(key)) match {
        case (Some(actualValue), Some(expectedValue)) =>
          if (actualValue != expectedValue)
            Some(Diff.Changed(key, actualValue, expectedValue))
          else
            None
        case (Some(actualValue), None) =>
          Some(Diff.Added(key, actualValue))
        case (None, Some(expectedValue)) =>
          Some(Diff.Removed(key, expectedValue))
        case (None, None) =>
          None
      }
    }
    result.flatten
  }
}

object Differ {

  private def toMap(obj: Any): Map[FieldPath, Any] = {
    case class FieldName(encodedName: String, decodedName: String)

    def caseClassFields(obj: Any): Seq[FieldName] = {
      val symbol = currentMirror.reflect(obj).symbol
      val params = symbol.primaryConstructor.asMethod.paramLists.flatten
      val result = params.map { param =>
        val name = param.name
        val encodedName = name.encodedName.toString
        val decodedName = name.decodedName.toString
        FieldName(encodedName, decodedName)
      }
      result
    }

    def isCaseClass(obj: Any): Boolean =
      if (obj != null) {
        val symbol = currentMirror.reflect(obj).symbol
        symbol.isCaseClass
      } else {
        false
      }

    def toMapOrIdentity(obj: Any): Any = obj match {
      case map: Map[_, _] =>
        map.map { case (k, v) => k -> toMapOrIdentity(v) }
      case seq: Seq[_] =>
        val map = seq.zipWithIndex.map(_.swap).toMap
        toMapOrIdentity(map)
      case array: Array[_] =>
        toMapOrIdentity(array.toSeq)
      case _ if isCaseClass(obj) =>
        val clz = obj.getClass
        val fields = caseClassFields(obj)
        if (fields.nonEmpty) {
          val map = caseClassFields(obj).map { name =>
            val value = clz.getMethod(name.encodedName).invoke(obj)
            name.decodedName -> value
          }.toMap
          toMapOrIdentity(map)
        } else { // case-class without fields or case-object
          obj
        }
      case _ =>
        obj
    }

    def flattenMap(path: FieldPath, value: Any): Map[FieldPath, Any] = value match {
      case map: Map[_, _] =>
        map.flatMap { case (k, v) =>
          flattenMap(path :+ k.toString, v)
        }
      case _ =>
        Map(path -> value)
    }

    val mapOrIdentity = toMapOrIdentity(obj)
    flattenMap(FieldPath.Empty, mapOrIdentity)
  }
}
