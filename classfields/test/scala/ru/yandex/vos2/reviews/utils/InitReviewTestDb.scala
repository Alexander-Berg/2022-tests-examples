package ru.yandex.vos2.reviews.utils

import org.scalatest.{OptionValues, Suite}
import ru.yandex.vos2.util.TimeWatcher
import ru.yandex.vos2.util.log.Logging

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 05/10/2017.
  */
trait InitReviewTestDb extends Suite with OptionValues with Logging with DockerReviewCoreComponents {

  private def readSqlFile(name: String) = {
    scala.io.Source.fromURL(getClass.getResource(name), "UTF-8")
      .getLines
      .filter(s => s.trim.nonEmpty && !s.trim.startsWith("--"))
      .map(s ⇒ s.split("--").head)
      .mkString("\n").split(";")
  }

  private def dropDb(name: String): String = {
    s"drop database if exists $name"
  }

  def initReviewsDbs(): Unit = {
    val time = TimeWatcher.withNanos()
    log.info("initNewOffersDbs started")
    val reviewsSchema = readSqlFile("/schema_base.sql")
    mySql.shards.map(shard => (shard.master, shard.index + 1)).foreach {
      case (database, index) =>
        // TODO дропать не базы, а таблицы
        val sql1 = dropDb(s"vos2_reviews")
        database.jdbc.update(sql1)
        val sql2 = s"create database if not exists vos2_reviews"
        database.jdbc.update(sql2)

        val sql3 = s"use vos2_reviews"
        database.jdbc.update(sql3)
        reviewsSchema.foreach(sql => {
          //log.info(s"${database.url} $sql")
          database.jdbc.update(sql)
        })
    }
    log.info(s"initReviewsDbs done in ${time.toMillis} ms.")
  }

}
