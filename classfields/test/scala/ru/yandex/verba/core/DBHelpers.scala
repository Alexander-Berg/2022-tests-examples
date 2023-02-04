package ru.yandex.verba.core

import ru.yandex.verba.core.util.{Logging, VerbaUtils}
import scalikejdbc.{DBSession, SQL}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.io.Source
import scala.reflect.{classTag, ClassTag}

/**
  * User: Vladislav Dolbilov (darl@yandex-team.ru)
  * Date: 30.04.13 2:35
  */
trait DBHelpers extends VerbaUtils { this: Logging =>

  implicit class DBString(sql: String)(implicit session: DBSession) {

    def getAs[A](implicit manifest: ClassTag[A]): A = {
      SQL(sql).map(rs => cast[A](rs.any(1))).single().apply().get
    }

    def execute(params: (Symbol, Any)*) = {
      SQL(sql).bindByName(params: _*).execute().apply()
    }
  }

  def load(file: String, params: (Symbol, String)*)(implicit session: DBSession): Unit = {
    load(Seq(file), params: _*)
  }

  def load(files: Seq[String], params: (Symbol, String)*)(implicit session: DBSession): Unit = {
    var loaded = Set.empty[String]
    val paramMap = mutable.HashMap() ++ params

    val Include = "--include (.*)".r
    val Bind = "--bind (.*?) (.*)$".r

    def loadFile(file: String): String = {
      if (loaded(file)) {
        ""
      } else {
        loaded += file
        using(Source.fromInputStream(getClass.getResourceAsStream(file))) { source =>
          source
            .getLines()
            .map {
              case Include(includeFile) => loadFile(s"/sql/$includeFile.sql")
              case Bind(main, r) =>
                val rest = r.split(" ")
                logger.trace(s"Matched: $main $rest")
                val Param = s"(.*)$main(.*)".r
                rest.foreach {
                  case param @ Param(prefix, suffix) =>
                    paramMap += Symbol(param) -> (prefix + paramMap.getOrElse(Symbol(main), main) + suffix)
                }
                ""
              case line => line
            }
            .mkString("\n")
        }
      }
    }
    @tailrec
    def expandParams(sql: String, params: List[Symbol]): String = {
      params match {
        case Nil => sql
        case param :: tail =>
          val name = param.name
          val newSql = sql.replaceAll(s"(?<=[\\s])$name(?=[\\s(])", paramMap(param))
          expandParams(newSql, tail)
      }
    }
    val loadedSql = files.map(loadFile).mkString
    logger.trace("Params: {}", paramMap)

    val sql = expandParams(loadedSql, paramMap.keys.toList)

    val statements =
      sql
        .split(";")
        .map(_.trim)
        .filter(_.nonEmpty)

    statements.foreach { statement =>
      logger.trace(s"Executing: $statement")
      SQL(statement).execute().apply()
    }
  }

  private def cast[A](v: Any)(implicit manifest: ClassTag[A]): A = {
    if (manifest == classTag[Int]) {
      v match {
        case null => 0
        case bigDecimal: java.math.BigDecimal => bigDecimal.intValue
        case bigDecimal: scala.math.BigDecimal => bigDecimal.toInt
        case int: java.lang.Integer => java.lang.Integer.parseInt(int.toString)
        case short: java.lang.Short => java.lang.Integer.parseInt(short.toString)
        case x => x
      }
    } else if (manifest == classTag[Long]) {
      v match {
        case null => 0L
        case bigDecimal: java.math.BigDecimal => bigDecimal.longValue
        case bigDecimal: scala.math.BigDecimal => bigDecimal.toLong
        case int: java.lang.Integer => java.lang.Long.parseLong(int.toString)
        case short: java.lang.Short => java.lang.Long.parseLong(short.toString)
        case x => x
      }
    } else if (manifest == classTag[String]) {
      v match {
        case null => null
        case o => String.valueOf(o)
      }
    } else {
      v
    }
  }.asInstanceOf[A]
}
