package ru.yandex.vertis.banker.dao.impl.jdbc

import java.util.concurrent.atomic.AtomicLong

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplateBase._
import slick.jdbc.JdbcBackend

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

/**
  * @author ruslansd
  */
trait JdbcSpecTemplateBase extends BeforeAndAfterAll with AsyncSpecBase {
  this: Suite =>

  private lazy val dbs = new ArrayBuffer[NamedDatabase]()

  protected def create(scheme: Scheme): JdbcBackend.Database = {
    val db = meter(JdbcContainerSpec.createDatabase(scheme), CreateSchemaTimeSpent) { time =>
      s"Total create scheme spent time: [${time / 1000 / 1000}] ms"
    }
    synchronized {
      dbs += db
    }
    db.database
  }

  override def afterAll(): Unit = {
    super.afterAll()
    val drop = Future.traverse(dbs)(db => JdbcContainerSpec.drop(db.name))
    meter(Await.result(drop, 10.seconds), DropDbTimeSpent) { time =>
      s"Total drop database spent time: [${time / 1000 / 1000}] ms"
    }: Unit
  }

  private def meter[A](f: => A, totalSpent: AtomicLong)(msg: Long => String) = {
    val start = System.nanoTime()
    val r = f
    val newValue = totalSpent.addAndGet(System.nanoTime() - start)
    log.info(msg(newValue))
    r
  }
}

object JdbcSpecTemplateBase {
  case class Scheme(schemePath: String, namePrefix: String)

  case class NamedDatabase(database: JdbcBackend.Database, name: String)

  private val CreateSchemaTimeSpent = new AtomicLong(0)
  private val DropDbTimeSpent = new AtomicLong(0)

  private val log = LoggerFactory.getLogger(classOf[JdbcSpecTemplateBase])
}
