package vertis.broker.yops.tasks.archive

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Message
import com.google.protobuf.util.Timestamps
import common.yt.Yt.Attribute.RowCount
import org.scalacheck.Gen
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.vertis.broker.model.common.PartitionPeriods
import ru.yandex.vertis.broker.model.common.PartitionPeriods._
import ru.yandex.vertis.broker.utils.ParsingUtils.TimestampFieldName
import ru.yandex.vertis.generators.ProducerProvider._
import vertis.broker.yops.tasks.archive.DayColumnMapper.dayColumnName
import vertis.broker.yops.tasks.archive.YtArchiver.SchemaVersionAttribute
import vertis.broker.yops.tasks.archive.YtArchiverIntSpec._
import vertis.broker.yops.tasks.archive.YtArchiverSpec.getDates
import vertis.broker.yops.tasks.tasks_common.Backup
import vertis.broker.yops.testkit.YtTasksTestSupport
import vertis.broker.yt.YtSchemaVersionAttributes.YtSchemaVersionsAttribute
import vertis.core.time.DateTimeUtils._
import vertis.proto.converter.YtTableTestHelper
import vertis.sraas.model.SchemaVersion
import vertis.yt.model.YPaths._
import vertis.yt.model.YtTable
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.BTask
import zio._
import zio.duration._

import java.time.LocalDate
import java.time.temporal.ChronoUnit._

/** @author kusaeva
  */
class YtArchiverIntSpec extends YtZioTest with YtTasksTestSupport with YtTableTestHelper {
  private val now = LocalDate.now
  private val year = now.getYear
  private val start = LocalDate.of(year - 2, 10, 1)

  private val backupPath = testBasePath.child(Backup.folderName)
  private val scriptsPath = testBasePath.child("scripts")

  override protected val ioTestTimeout: Duration = 15.minutes

  override def basePath: YPath = testBasePath.child("warehouse")

  override def beforeAll(): Unit = {
    super.beforeAll()
    ioTest {
      ytZio.use { yt =>
        yt.cypressNoTx.touchDir(scriptsPath) *>
          yt.filesNoTx.write(
            scriptsPath.child(DayColumnMapper.addDayColumnScript),
            getClass.getResourceAsStream("/jobs/add_day_column.py")
          )
      }
    }
  }

  "YtArchiver" should {
    "archive" in ioTest {
      ytResources.map(_.yt).use { yt =>
        val archiver = new YtArchiver(yt, basePath, doDrop = true)

        val sourcePartitioning = PartitionPeriods.byMonth
        val targetPartitioning = PartitionPeriods.byYear

        val dates = getDates(start, now.minusMonths(1), sourcePartitioning)
        val years = (start.getYear to year).map(LocalDate.ofYearDay(_, 1)).toSet
        val last = sourcePartitioning.round(now)
        val archiveDelay = 1
        for {
          name <- randomName
          job = ArchiveJob(name, sourcePartitioning, targetPartitioning, archiveDelay, tsColumn = "timestamp", Nil)
          sourcePath = basePath.child(name).child(sourcePartitioning.short)
          targetPath = basePath.child(name).child(targetPartitioning.short)

          _ <- createDayTables(yt, sourcePath, dates, ArchiveMessageV2.javaDescriptor, v1)
          /* create table that will not be archived */
          _ <- createDayTable(yt, last, sourcePath, ArchiveMessageV1.javaDescriptor, v1)
          _ <- archiver.process(job)
          _ <- checkM("archive tables:") {
            yt.cypressNoTx
              .listDays(targetPath)
              .map(_.keySet should contain theSameElementsAs years)
          }
          _ <- checkM("drop only archived tables:") {
            yt.cypressNoTx
              .listDays(sourcePath)
              .map(_.keys should contain theSameElementsAs Seq(last))
          }
        } yield ()
      }
    }

    "migrate" in ioTest {
      ytResources.map(_.yt).use { yt =>
        val archiver = new YtArchiver(yt, basePath, doMigrate = true, doDrop = true)

        val sourcePartitioning = PartitionPeriods.byMonth
        val targetPartitioning = PartitionPeriods.byYear

        val dates = getDates(start, now, sourcePartitioning)
        val years = (start.getYear to year).map(LocalDate.ofYearDay(_, 1)).toSet
        val descriptor = ArchiveMessageV2.javaDescriptor

        for {
          name <- randomName
          job =
            ArchiveJob(
              name,
              sourcePartitioning,
              targetPartitioning,
              archiveDelay = 0,
              tsColumn = "timestamp",
              Seq("num")
            )
          sourcePath = basePath.child(name).child(sourcePartitioning.short)
          targetPath = basePath.child(name).child(targetPartitioning.short)
          targetName = targetPartitioning.round(now)
          /* create year table */
          _ <- createDayTable(yt, targetName, targetPath, descriptor, v1)
          _ <- createDayTables(yt, sourcePath, dates, descriptor, v1)
          _ <- archiver.process(job)
          _ <- checkM("migrate tables:") {
            yt.cypressNoTx
              .listDays(targetPath)
              .map(_.keySet should contain theSameElementsAs years)
          }
        } yield ()
      }
    }

    "archive to an existent table" in ioTest {
      ytResources.map(_.yt).use { yt =>
        val archiver = new YtArchiver(yt, basePath, doDrop = true)

        val sourcePartitioning = PartitionPeriods.byDay
        val targetPartitioning = PartitionPeriods.byYear

        val archiveDelay = 1
        val oldRowsCount = 5
        val newRowsCount = 3
        for {
          name <- randomName
          job = ArchiveJob(name, sourcePartitioning, targetPartitioning, archiveDelay, tsColumn = "timestamp", Nil)
          sourcePath = basePath.child(name).child(sourcePartitioning.short)
          targetPath = basePath.child(name).child(targetPartitioning.short)
          _ <- { // create target table
            val descriptor = ArchiveMessageV1.javaDescriptor
            val date = now.withDayOfYear(1)
            createDayTable(yt, date, targetPath, descriptor, v1, Some(oldRowsCount))
          }
          _ <- { // create table to archive
            val descriptor = ArchiveMessageV2.javaDescriptor
            val date = now.minusDays(1)
            createDayTable(yt, date, sourcePath, descriptor, v2, Some(newRowsCount))
          }
          _ <- { // create table that will not be archived
            val descriptor = ArchiveMessageV3.javaDescriptor
            val date = now
            createDayTable(yt, date, sourcePath, descriptor, v3)
          }
          _ <- archiver.process(job)
          result <- yt.cypressNoTx
            .listDays(targetPath, Seq(SchemaVersionAttribute, RowCount.name))
            .map(_.headOption)
          _ <- check("archive tables:") {
            result shouldBe defined
            val (day, (_, attrMap)) = result.get
            day shouldBe now.withDayOfYear(1)
            attrMap(YtSchemaVersionsAttribute) should contain theSameElementsAs Seq(v1, v2)
            attrMap(RowCount) shouldBe (oldRowsCount + newRowsCount)
          }
          _ <- checkM("all columns in schema:") {
            yt.tables
              .getSchema(None, targetPath.dayChild(targetPartitioning.round(now)))
              .map(
                _.columnNames should contain theSameElementsAs Seq(dayColumnName, "timestamp", "num", "str", "inner")
              )
          }
        } yield ()
      }
    }

    "archive tables with different versions" in ioTest {
      ytResources.map(_.yt).use { yt =>
        val archiver = new YtArchiver(yt, basePath, doDrop = true)

        val sourcePartitioning = PartitionPeriods.byDay
        val targetPartitioning = PartitionPeriods.byYear

        val archiveDelay = 0

        for {
          name <- randomName
          sourcePath = basePath.child(name).child(sourcePartitioning.short)
          targetPath = basePath.child(name).child(targetPartitioning.short)
          _ <- { // create table to archive
            val descriptor = ArchiveMessageV1.javaDescriptor
            val date = now.minusDays(2)
            createDayTable(yt, date, sourcePath, descriptor, v1)
          }
          _ <- { // create table with another version to archive
            val descriptor = ArchiveMessageV2.javaDescriptor
            val date = now.minusDays(1)
            createDayTable(yt, date, sourcePath, descriptor, v2)
          }
          _ <- { // create table with another version to archive
            val descriptor = ArchiveMessageV3.javaDescriptor
            val date = now
            createDayTable(yt, date, sourcePath, descriptor, v3)
          }
          job = ArchiveJob(name, sourcePartitioning, targetPartitioning, archiveDelay, tsColumn = "timestamp", Nil)
          _ <- archiver.process(job)
          result <- yt.cypressNoTx
            .listDays(targetPath, Seq(SchemaVersionAttribute))
            .map(_.headOption)
          _ <- check("archive tables:") {
            result shouldBe defined
            val (day, (_, attrMap)) = result.get
            day shouldBe now.withDayOfYear(1)
            attrMap(YtSchemaVersionsAttribute) should contain theSameElementsAs Seq(v1, v2, v3)
          }
          _ <- checkM("all columns in schema:") {
            yt.tables
              .getSchema(None, targetPath.dayChild(targetPartitioning.round(now)))
              .map(
                _.columnNames should contain theSameElementsAs Seq(
                  dayColumnName,
                  "timestamp",
                  "num",
                  "str",
                  "bool",
                  "inner"
                )
              )
          }
        } yield ()
      }
    }

    "archive links" in ioTest {
      ytResources.map(_.yt).use { yt =>
        val archiver = new YtArchiver(yt, basePath, doDrop = true)

        val sourcePartitioning = PartitionPeriods.byDay
        val targetPartitioning = PartitionPeriods.byYear

        val archiveDelay = 1
        val last = sourcePartitioning.round(now)

        for {
          name <- randomName
          sourcePath = basePath.child(name).child(sourcePartitioning.short)
          targetPath = basePath.child(name).child(targetPartitioning.short)
          originPath = basePath.parent().child("origin").child(name).child(sourcePartitioning.short)
          _ <- { // create link table to archive
            val descriptor = ArchiveMessageV1.javaDescriptor
            val date = now.minusDays(3)
            createOriginTable(yt, date, originPath, sourcePath, descriptor, v1)
          }
          _ <- { // create table with another version to archive
            val descriptor = ArchiveMessageV3.javaDescriptor
            val date = now.minusDays(2)
            createDayTable(yt, date, sourcePath, descriptor, v2)
          }
          _ <- { // create another link table with another version to archive
            val descriptor = ArchiveMessageV2.javaDescriptor
            val date = now.minusDays(1)
            createOriginTable(yt, date, originPath, sourcePath, descriptor, v2)
          }
          _ <- { // create another link table that will not be archived
            val descriptor = ArchiveMessageV3.javaDescriptor
            createOriginTable(yt, last, originPath, sourcePath, descriptor, v3)
          }
          job = ArchiveJob(name, sourcePartitioning, targetPartitioning, archiveDelay, tsColumn = "timestamp", Nil)
          _ <- archiver.process(job)
          result <- yt.cypressNoTx
            .listDays(targetPath, Seq(SchemaVersionAttribute))
            .map(_.headOption)
          _ <- check("archive tables:") {
            result shouldBe defined
            val (day, (_, attrMap)) = result.get
            day shouldBe now.withDayOfYear(1)
            attrMap(YtSchemaVersionsAttribute) should contain theSameElementsAs Seq(v1, v2)
          }
          _ <- checkM("all columns in schema:") {
            yt.tables
              .getSchema(None, targetPath.dayChild(targetPartitioning.round(now)))
              .map(
                _.columnNames should contain theSameElementsAs Seq(
                  dayColumnName,
                  "timestamp",
                  "num",
                  "str",
                  "bool",
                  "inner"
                )
              )
          }
          _ <- checkM("all links dropped:") {
            yt.cypressNoTx
              .listDays(sourcePath)
              .map(_.keys should contain theSameElementsAs Seq(last))
          }
          _ <- checkM("origin tables preserved:") {
            yt.cypressNoTx
              .listDays(originPath)
              .map(_.keys should contain theSameElementsAs Seq(last, now.minusDays(3), now.minusDays(1)))
          }
        } yield ()
      }
    }
  }

  private def createDayTable(
      yt: YtZio,
      day: LocalDate,
      basePath: YPath,
      descriptor: Descriptor,
      version: SchemaVersion,
      rowCount: Option[Int] = None): BTask[YtTable] = {
    val path = basePath.dayChild(day)
    val n = rowCount.getOrElse(Gen.choose(1, 5).next)
    val rows = getMessages(descriptor, day, n)
    val attrs = Seq(YtSchemaVersionsAttribute.attribute(List(version).map(_.toString)))

    logger.info(s"Create table at $path with $version") *>
      createTable(yt, path, rows, descriptor, attrs, Seq.empty)
  }

  private def createDayTables(
      yt: YtZio,
      path: YPath,
      dates: Seq[LocalDate],
      descriptor: Descriptor,
      schemaVersion: SchemaVersion,
      parN: Int = 8) =
    ZIO.foreachParN_(parN)(dates) { date =>
      createDayTable(yt, date, path, descriptor, schemaVersion)
    }

  private def createOriginTable(
      yt: YtZio,
      day: LocalDate,
      basePath: YPath,
      linkPath: YPath,
      descriptor: Descriptor,
      version: SchemaVersion,
      rowCount: Option[Int] = None): BTask[YtTable] =
    createDayTable(yt, day, basePath, descriptor, version, rowCount)
      .tap { table =>
        val path = linkPath.dayChild(day)
        yt.cypressNoTx.link(table.path, path, recursive = true) *>
          yt.cypressNoTx.setAttribute(path.asLink, YtSchemaVersionsAttribute.attribute(List(version).map(_.toString)))
      }

  private def getMessages(descriptor: Descriptor, day: LocalDate, n: Int): Iterable[Message] = {
    val tsFd = descriptor.findFieldByName(TimestampFieldName)

    messagesGen(descriptor)
      .next(n)
      .zipWithIndex
      .map { case (m, i) =>
        val ts = toInstant(day).plus(i.toLong, MINUTES)
        m.toBuilder
          .setField(tsFd, Timestamps.fromMillis(ts.toEpochMilli))
          .build()
      }
  }
}

object YtArchiverIntSpec {
  private val v1 = SchemaVersion(0, 0, 1)
  private val v2 = SchemaVersion(0, 0, 2)
  private val v3 = SchemaVersion(0, 1, 0)
}
