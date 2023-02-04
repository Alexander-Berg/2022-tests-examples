package vertis.stream.yt.init

import java.time.LocalDate
import com.google.protobuf.Descriptors.Descriptor
import common.sraas.Sraas.SraasDescriptor
import common.sraas.{Sraas, TestSraas}
import ru.yandex.vertis.broker.model.common.PartitionPeriods
import vertis.proto.converter.ProtoYsonConverterImpl
import vertis.stream.yt.conf.YtStreamSinkConfig
import vertis.stream.yt.test.evolution._
import vertis.yt.zio.YtZioTest
import vertis.yt.model.YPaths.RichYPath
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.BTask
import vertis.zio.test.ZioSpecBase.TestBody
import zio.{Task, UIO, ZIO}
import zio.interop.catz._

class YtTableInitIntSpec extends YtZioTest {

  private val converter = new ProtoYsonConverterImpl[BTask]()

  private val path = testBasePath
  private val messageType = "tstst"

  private val day = LocalDate.now()

  private val oldVersions = Set("v0.0.1")
  private val newVersions = Set("v0.1.0")

  private val oldColumns = Seq("timestamp", "persistent", "gone", "renamed")
  private val newColumns = Seq("timestamp", "persistent", "renamed_to")
  private val allColumns = (oldColumns ++ newColumns).distinct

  private val sinkConfig = YtStreamSinkConfig(
    path,
    "test",
    PartitionPeriods.byDay,
    messageType,
    None
  )

  case class TestEnv(yt: YtZio) {

    def createInit(descriptor: Descriptor): Task[YtTableInit] =
      (for {
        sraas <- ZIO.service[Sraas.Service]
        _ <- TestSraas.setJavaDescriptor(_ => UIO(SraasDescriptor(descriptor, "test", "version")))
      } yield new YtTableInit(yt, converter, sraas)).provideLayer(env ++ TestSraas.layer)
  }

  def testEnv(f: TestEnv => TestBody): Unit = ioTest {
    ytZio.use { yt =>
      f(TestEnv(yt))
    }
  }

  "YtTableInit" should {

    "create new table" in testEnv { env =>
      for {
        init <- env.createInit(OldVersion.javaDescriptor)
        res <- env.yt.tx.withTx("test") {
          init.prepareDayTable(
            sinkConfig,
            day,
            oldVersions,
            None
          )
        }
        _ <- check {
          res.versions should contain theSameElementsAs oldVersions
          res.schema.columnNames should contain theSameElementsAs oldColumns
        }
        schemaFromYt <- env.yt.tables.getSchema(None, sinkConfig.tableBase.dayChild(day))
      } yield {
        schemaFromYt.columnNames should contain theSameElementsAs oldColumns
      }
    }

    "not touch table if version hasn't changed" in testEnv { env =>
      for {
        init <- env.createInit(OldVersion.javaDescriptor)
        res <- env.yt.tx.withTx("test") {
          init.prepareDayTable(
            sinkConfig,
            day,
            oldVersions,
            None
          )
        }
        _ <- check {
          res.versions should contain theSameElementsAs oldVersions
          res.schema.columnNames should contain theSameElementsAs oldColumns
        }
        schemaFromYt <- env.yt.tables.getSchema(None, sinkConfig.tableBase.dayChild(day))
      } yield {
        schemaFromYt.columnNames should contain theSameElementsAs oldColumns
      }
    }

    "merge schema if version has changed" in testEnv { env =>
      for {
        init <- env.createInit(NewVersion.javaDescriptor)
        res <- env.yt.tx.withTx("test") {
          init.prepareDayTable(
            sinkConfig,
            day,
            newVersions,
            None
          )
        }
        _ <- check {
          res.versions should contain theSameElementsAs oldVersions ++ newVersions
          res.schema.columnNames should contain theSameElementsAs allColumns
        }
        schemaFromYt <- env.yt.tables.getSchema(None, sinkConfig.tableBase.dayChild(day))
      } yield {
        schemaFromYt.columnNames should contain theSameElementsAs allColumns
      }
    }

    "support backward evolution" in testEnv { env =>
      val day = LocalDate.now().minusDays(4)
      for {
        init <- env.createInit(NewVersion.javaDescriptor)
        res <- env.yt.tx.withTx("test") {
          init.prepareDayTable(
            sinkConfig,
            day,
            newVersions,
            None
          )
        }
        _ <- check {
          res.versions should contain theSameElementsAs newVersions
          res.schema.columnNames should contain theSameElementsAs newColumns
        }

        init2 <- env.createInit(OldVersion.javaDescriptor)
        res2 <- env.yt.tx.withTx("test") {
          init2.prepareDayTable(
            sinkConfig,
            day,
            oldVersions,
            None
          )
        }
        _ <- check {
          res2.versions should contain theSameElementsAs oldVersions ++ newVersions
          res2.schema.columnNames should contain theSameElementsAs allColumns
        }
        schemaFromYt <- env.yt.tables.getSchema(None, sinkConfig.tableBase.dayChild(day))
      } yield {
        schemaFromYt.columnNames should contain theSameElementsAs allColumns
      }
    }
  }
}
