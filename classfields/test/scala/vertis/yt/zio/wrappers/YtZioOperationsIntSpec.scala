package vertis.yt.zio.wrappers

import common.yt.schema.YtTypes
import org.scalatest.ParallelTestExecution
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.common.http.EmptyCompressor
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import vertis.yt.model.{YtColumn, YtSchema, YtTable}
import vertis.yt.util.matchers.YtMatchers
import vertis.yt.util.support.YsonSupport
import vertis.yt.zio.Aliases.YtTask
import vertis.yt.zio.YtZioTest
import zio.duration._

import scala.util.Random

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class YtZioOperationsIntSpec extends YtZioTest with YtMatchers with YsonSupport with ParallelTestExecution {

  "yt operations" should {

    "await result" in ioTest {
      ytZio.use { yt =>
        val rowsToWrite = 10
        for {
          table <- createTable(yt, testBasePath.child("to_erase"), rowsToWrite)
          rowCount <- yt.cypress.getAttribute(None, table.path, "row_count").map(_.intValue())
          _ <- yt.operations.erase(None, table.path, Some(100.millis)).retry(noScheduler)
          tableExists <- yt.cypress.exists(None, table.path)
          erasedRowCount <- yt.cypress.getAttribute(None, table.path, "row_count").map(_.intValue())
          _ <- check("table still exists")(tableExists shouldBe true)
          _ <- check("table had rows")(rowCount shouldBe rowsToWrite)
          _ <- check("table is now empty")(erasedRowCount shouldBe 0)
        } yield ()
      }

    }
  }

  private def createTable(yt: YtZio, path: YPath, rows: Int): YtTask[YtTable] = {
    val table = YtTable(path.name(), path, YtSchema(Seq(YtColumn("payload", YtTypes.string))))
    val append = yt.tables.appendToTable(None, YTableEntryTypes.YSON)(
      table.path,
      Iterator
        .continually(Random.nextString(100))
        .map(str => yTreeMap("payload" -> YTree.stringNode(str)))
        .take(rows),
      new EmptyCompressor()
    )
    yt.cypress.createTable(None, table, ignoreExisting = false) *> append.as(table)
  }
}
