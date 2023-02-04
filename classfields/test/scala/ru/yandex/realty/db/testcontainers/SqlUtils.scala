package ru.yandex.realty.db.testcontainers

import scala.io.Source
import scala.util.Try
import scala.util.matching.Regex

object SqlUtils {

  val DefaultDelimiter = ";"
  val DelimiterPattern: Regex = "DELIMITER\\s+(.*)".r
  val DelimiterGroup: Int = 1

  def loadScript(filename: String): Try[Seq[String]] =
    Try {
      val schemaScript = Source.fromResource(filename).mkString

      val delimiters = DefaultDelimiter +: DelimiterPattern
        .findAllMatchIn(schemaScript)
        .map(_.group(DelimiterGroup))
        .toSeq

      val parts = DelimiterPattern.split(schemaScript).toSeq
      delimiters.zip(parts) flatMap {
        case (delimiter, part) =>
          part
            .split(delimiter)
            .map(_.trim)
            .filter(_.nonEmpty)
      }
    }.recoverWith {
      case err => scala.util.Failure(new RuntimeException(s"Can't load sql script from file [$filename]", err))
    }
}
