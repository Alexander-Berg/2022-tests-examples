package vertis.yt.hooch.testkit

import common.yt.YtError

import java.time.LocalDate
import ru.yandex.inside.yt.kosher.cypress.YPath
import vertis.yt.model.YPaths.RichYPath
import vertis.yt.model.{YtSchema, YtTable}
import vertis.yt.zio.Aliases.YtRIO
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.wrappers.YtZioCypressOptTx.YtZioCypressNoTx
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase.TestBody
import zio.ZIO

/**
  */
trait BaseHoochYtSpec extends YtZioTest {

  protected val basePath: YPath = testBasePath

  protected case class TestEnv(yt: YtZioCypressNoTx) {

    def makeDayTables(tablePath: YPath, days: LocalDate*): ZIO[BaseEnv, YtError, Unit] = {
      yt.touchDir(tablePath) *> {
        ZIO.foreach(days) { day =>
          val table = YtTable(s"test $day", tablePath.dayChild(day), YtSchema.Empty)
          yt.createTable(table, ignoreExisting = false)
        }
      }
    }.unit

    def makeTable(tablePath: YPath): YtRIO[BaseEnv, Unit] = {
      yt.touchDir(tablePath.parent()) *> {
        val table = YtTable("test", tablePath, YtSchema.Empty)
        yt.createTable(table, ignoreExisting = false)
      }
    }
  }

  protected def testEnv(f: TestEnv => TestBody): Unit =
    ioTest {
      ytZio.use { yt =>
        val env = TestEnv(yt.cypressNoTx)
        f(env)
      }
    }

}
