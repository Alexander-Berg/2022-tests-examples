package ru.yandex.vertis.billing.dao.impl.jdbc

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplateBase._
import slick.jdbc.JdbcBackend

import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable.ArrayBuffer

/**
  * @author ruslansd
  */
trait JdbcSpecTemplateBase extends BeforeAndAfterAll {
  this: Suite =>

  private lazy val dbs = new ArrayBuffer[NamedDatabase]()

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true

  protected def createNamed(scheme: Scheme): NamedDatabase = {
    val db = meter(JdbcContainerSpec.database(scheme), CreateSchemaTimeSpent) { time =>
      s"Total create scheme spent time: [${time / 1000 / 1000}] ms"
    }
    synchronized {
      dbs += db
    }
    db
  }

  protected def create(scheme: Scheme): JdbcBackend.Database =
    createNamed(scheme: Scheme).database

  override def afterAll(): Unit = {
    super.afterAll()
    meter(dbs.foreach(db => JdbcContainerSpec.drop(db.name)), DropDbTimeSpent) { time =>
      s"Total drop database spent time: [${time / 1000 / 1000}] ms"
    }
    dbs.foreach(_.database.close())
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
