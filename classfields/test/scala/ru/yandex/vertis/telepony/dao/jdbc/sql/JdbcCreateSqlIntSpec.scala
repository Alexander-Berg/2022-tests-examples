package ru.yandex.vertis.telepony.dao.jdbc.sql

import java.io.File

import org.scalatest.BeforeAndAfterAll
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.jdbc.api.{Database, _}
import ru.yandex.vertis.telepony.util.JdbcBuilder
import slick.dbio.DBIOAction
import slick.jdbc._

import scala.io.Source

/**
  * Checks  telepony.sql == 000.sql & 001.sql & ...
  *
  * @author evans
  */
abstract class JdbcCreateSqlIntSpec extends SpecBase with BeforeAndAfterAll {

  import scala.concurrent.ExecutionContext.Implicits.global

  def sqlBaseDir: String

  val dbFinal: Database = new JdbcBuilder {
    override def databaseName: String = s"telepony_unit_test_dbFinal"

    def schemaScript: String =
      Source.fromInputStream(getClass.getResourceAsStream(s"$sqlBaseDir/telepony.sql")).mkString
  }.createSimpleDualDatabase().master.db

  val dbSteps: Database = new JdbcBuilder {
    override def databaseName: String = s"telepony_unit_test_dbSteps"

    private val sqlFolder = new File(getClass.getResource(sqlBaseDir).getPath)
    private val sqlFiles = sqlFolder.listFiles().toList.filter(_.getName.matches("\\d{3}\\.sql")).sortBy(_.getName)

    private val schema = sqlFiles
      .map { f =>
        val source = Source.fromFile(f)
        try {
          source.mkString
        } finally {
          source.close()
        }
      }
      .mkString("\n")

    def schemaScript: String = schema
  }.createSimpleDualDatabase().master.db

  "Jdbc" should {
    "have same ddl for final.sql and partial sqls" in {
      val finalTables = dbFinal
        .run(getTables)
        .futureValue
        .map { t =>
          t.name -> t.ddl
        }
        .toMap
      val stepsTables = dbSteps
        .run(getTables)
        .futureValue
        .map { t =>
          t.name -> t.ddl
        }
        .toMap
      finalTables.keySet shouldEqual stepsTables.keySet
      finalTables.keys.foreach { n =>
        val finalTable = finalTables.get(n)
        finalTable.map(ddlUnified).shouldEqual(stepsTables.get(n).map(ddlUnified))
      }
    }
  }

  private def ddlUnified(s: String) =
    s.replaceAll("[, ]", "").split("\n").filter(_.nonEmpty).sorted.mkString("\n")

  implicit object GetTable extends GetResult[Table] {

    def apply(v: PositionedResult): Table = {
      val name = v.nextString()
      val ddl = v.nextString()
      Table(name, ddl)
    }
  }

  private def getTable(name: String) = sql"SHOW CREATE TABLE #$name".as[Table].head

  private def getTables = {
    val tableNames = sql"SHOW TABLES".as[String]
    tableNames.flatMap { names =>
      DBIOAction.sequence(names.map(getTable))
    }
  }.withPinnedSession

  override protected def afterAll(): Unit = {
    dbFinal.close()
    dbSteps.close()
    super.afterAll()
  }
}

case class Table(name: String, ddl: String)
