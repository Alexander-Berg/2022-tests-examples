package ru.yandex.vertis.scoring.tasks

import cats.data.NonEmptyList
import cats.effect.Clock
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, CompositeFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.feature_registry_utils.FeatureRegistryF
import ru.yandex.vertis.quality.scheduler_utils.SchedulerTask
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.scoring.config.DiffTaskConfig
import ru.yandex.vertis.scoring.dao.analyst.AnalystDiffDao
import ru.yandex.vertis.scoring.dao.model.SummaryUpdate.AnalystData
import ru.yandex.vertis.scoring.dao.model.YtTable
import ru.yandex.vertis.scoring.dao.summary.SummaryDao
import ru.yandex.vertis.scoring.model.PhoneIds
import ru.yandex.vertis.scoring.zookeeper.UserScoringFeatureTypes
import eu.timepit.refined.auto._
import org.mockito.ArgumentCaptor
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.{IntFeatureType, StringFeatureType}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import ru.yandex.vertis.scoring.tasks.CalculateAndLoadDiffToYdbTask._

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.Instant
import scala.jdk.CollectionConverters.ListHasAsScala

class CalculateAndLoadDiffToYdbTaskSpec extends SpecBase {

  private val now = Instant.now()
  private val analystData: AnalystData =
    AnalystData(
      1234567L,
      diskActivity = false,
      edaActivity = true,
      edaUser = false,
      edaUserBlocked = true,
      kinopoiskActivity = false,
      kinopoiskUser = true,
      lavkaActivity = false,
      lavkaUser = true,
      lavkaUserBlocked = false,
      musicActivity = true,
      personalPhoneIds = Some(PhoneIds(NonEmptyList.of("hash0", "hash1"))),
      reviewsActivity = true,
      reviewsUser = false,
      taxiActivity = true,
      taxiUser = false,
      taxiUserBlocked = true,
      updateTime = now
    )
  private val batchSize = 100
  private val fullBatch: List[AnalystData] = (1 to batchSize).map(_ => analystData).toList
  private val fullBatchNonEmpty: NonEmptyList[AnalystData] = NonEmptyList.fromListUnsafe(fullBatch)
  private val notFullBatch: List[AnalystData] = (1 to batchSize / 2).map(_ => analystData).toList
  private val notFullBatchNonEmpty: NonEmptyList[AnalystData] = NonEmptyList.fromListUnsafe(notFullBatch)

  private val config =
    DiffTaskConfig(
      sourceAnalystDataFolder = "//home/source",
      preparedDataFolder = "//home/prepare"
    )
  private val inMemoryFeatureRegistry =
    new InMemoryFeatureRegistry(
      new CompositeFeatureTypes(
        Iterable(BasicFeatureTypes, UserScoringFeatureTypes)
      )
    )
  implicit private val featureRegistry: FeatureRegistryF[F] = new FeatureRegistryF[F](inMemoryFeatureRegistry)
  private val lastTableFeature = inMemoryFeatureRegistry.register(LastTableNameFeature, AbsentTable)
  private val batchSizeFeature = inMemoryFeatureRegistry.register(BatchSizeFeature, batchSize)

  "CalculateAndLoadDiffToYdbTask" should {
    "do nothing if last table in zk equals last table in yt" in {
      val analystDao = mock[AnalystDiffDao[F]]
      val summaryDao = mock[SummaryDao[F]]
      val lastTable = "2020-12-12_11:00:00"
      val tablesInYt =
        List(
          sourceYtTable("2020-12-11_23:00:00"),
          sourceYtTable(lastTable)
        ).pure[F]

      inMemoryFeatureRegistry.updateFeature(LastTableNameFeature, lastTable).futureValue
      when(analystDao.allTablesInFolder(eq(config.sourceAnalystDataFolder)))
        .thenReturn(tablesInYt)

      runTask(analystDao, summaryDao)

      lastTableFeature() shouldBe lastTable
      verify(summaryDao, never()).putBulkSummaryUpdates(?)
      verify(analystDao, never()).groupByPuid(?, ?, ?)
      verify(analystDao, never()).calculateDiff(?, ?, ?, ?)
      verify(analystDao, never()).getDiff(?, ?, ?)
    }

    "fail if source folder is empty" in {
      val analystDao = mock[AnalystDiffDao[F]]
      val summaryDao = mock[SummaryDao[F]]
      val tablesInYt = List.empty[YtTable].pure[F]

      when(analystDao.allTablesInFolder(eq(config.sourceAnalystDataFolder)))
        .thenReturn(tablesInYt)

      assertThrows[IllegalStateException](runTask(analystDao, summaryDao))

      verify(summaryDao, never()).putBulkSummaryUpdates(?)
      verify(analystDao, never()).groupByPuid(?, ?, ?)
      verify(analystDao, never()).calculateDiff(?, ?, ?, ?)
      verify(analystDao, never()).getDiff(?, ?, ?)
    }

    //two source tables can be equal so diff will be empty
    "load empty diff" in {
      val analystDao = mock[AnalystDiffDao[F]]
      val summaryDao = mock[SummaryDao[F]]
      val previousTableInYt = "2020-12-12_11:00:00"
      val lastTableInYt = "2020-12-12_23:00:00"
      val preparedYtTable = prepareYtTable(withDiffPrefix(previousTableInYt, lastTableInYt))
      val tablesInYt =
        List(
          sourceYtTable("2020-12-11_23:00:00"),
          sourceYtTable(previousTableInYt),
          sourceYtTable(lastTableInYt)
        ).pure[F]

      inMemoryFeatureRegistry.updateFeature(LastTableNameFeature, previousTableInYt).futureValue
      when(analystDao.allTablesInFolder(eq(config.sourceAnalystDataFolder)))
        .thenReturn(tablesInYt)
      when(
        analystDao.calculateDiff(
          eq(sourceYtTable(previousTableInYt)),
          eq(sourceYtTable(lastTableInYt)),
          eq(preparedYtTable),
          ?
        )
      ).thenReturn(().pure[F])
      when(
        analystDao.getDiff(
          eq(preparedYtTable),
          eq(0),
          eq(100)
        )
      ).thenReturn(List.empty.pure[F])
      when(analystDao.dropTable(eq(preparedYtTable)))
        .thenReturn(().pure[F])

      runTask(analystDao, summaryDao)

      lastTableFeature() shouldBe lastTableInYt
      verify(summaryDao, never()).putBulkSummaryUpdates(?)
      verify(analystDao, times(1)).calculateDiff(
        eq(sourceYtTable(previousTableInYt)),
        eq(sourceYtTable(lastTableInYt)),
        eq(preparedYtTable),
        ?
      )
      verify(analystDao, times(1)).getDiff(
        eq(preparedYtTable),
        eq(0),
        eq(100)
      )
    }

    "load one diff: full batch and one not full batch" in {
      val analystDao = mock[AnalystDiffDao[F]]
      val summaryDao = mock[SummaryDao[F]]
      val previousTableInYt = "2020-12-12_11:00:00"
      val lastTableInYt = "2020-12-12_23:00:00"
      val preparedYtTable = prepareYtTable(withDiffPrefix(previousTableInYt, lastTableInYt))
      val tablesInYt =
        List(
          sourceYtTable("2020-12-11_23:00:00"),
          sourceYtTable(previousTableInYt),
          sourceYtTable(lastTableInYt)
        ).pure[F]

      inMemoryFeatureRegistry.updateFeature(LastTableNameFeature, previousTableInYt).futureValue
      when(analystDao.allTablesInFolder(eq(config.sourceAnalystDataFolder)))
        .thenReturn(tablesInYt)
      when(
        analystDao.calculateDiff(
          eq(sourceYtTable(previousTableInYt)),
          eq(sourceYtTable(lastTableInYt)),
          eq(preparedYtTable),
          ?
        )
      ).thenReturn(().pure[F])
      when(
        analystDao.getDiff(
          eq(preparedYtTable),
          eq(0),
          eq(100)
        )
      ).thenReturn(fullBatch.pure[F])
      when(
        analystDao.getDiff(
          eq(preparedYtTable),
          eq(100),
          eq(100)
        )
      ).thenReturn(notFullBatch.pure[F])
      when(analystDao.dropTable(eq(preparedYtTable)))
        .thenReturn(().pure[F])
      when(summaryDao.putBulkSummaryUpdates(eq(fullBatchNonEmpty)))
        .thenReturn(().pure[F])
      when(summaryDao.putBulkSummaryUpdates(eq(notFullBatchNonEmpty)))
        .thenReturn(().pure[F])

      runTask(analystDao, summaryDao)

      lastTableFeature() shouldBe lastTableInYt
      verify(analystDao, times(1)).calculateDiff(
        eq(sourceYtTable(previousTableInYt)),
        eq(sourceYtTable(lastTableInYt)),
        eq(preparedYtTable),
        ?
      )
      verify(analystDao, times(2)).getDiff(
        eq(preparedYtTable),
        ?,
        ?
      )
      val argumentCaptor = ArgumentCaptor.forClass(classOf[NonEmptyList[AnalystData]])
      verify(summaryDao, times(2)).putBulkSummaryUpdates(argumentCaptor.capture())
      argumentCaptor.getAllValues.asScala.toList shouldBe List(fullBatchNonEmpty, notFullBatchNonEmpty)
    }

    "group by puid one table if last table from zk equals empty string" in {
      val analystDao = mock[AnalystDiffDao[F]]
      val summaryDao = mock[SummaryDao[F]]
      val lastTable = "2020-12-12_11:00:00"
      val sourceYtLastTable = sourceYtTable(lastTable)
      val preparedYtTable = prepareYtTable(withGroupedPrefix(lastTable))
      val tablesInYt = List(sourceYtLastTable).pure[F]

      inMemoryFeatureRegistry.updateFeature(LastTableNameFeature, AbsentTable).futureValue
      when(analystDao.allTablesInFolder(eq(config.sourceAnalystDataFolder)))
        .thenReturn(tablesInYt)
      when(
        analystDao.groupByPuid(
          eq(sourceYtLastTable),
          eq(preparedYtTable),
          ?
        )
      ).thenReturn(().pure[F])
      when(
        analystDao.getDiff(
          eq(preparedYtTable),
          eq(0),
          eq(100)
        )
      ).thenReturn(fullBatch.pure[F])
      when(
        analystDao.getDiff(
          eq(preparedYtTable),
          eq(100),
          eq(100)
        )
      ).thenReturn(List.empty.pure[F])
      when(analystDao.dropTable(eq(preparedYtTable)))
        .thenReturn(().pure[F])
      when(summaryDao.putBulkSummaryUpdates(eq(fullBatchNonEmpty)))
        .thenReturn(().pure[F])

      runTask(analystDao, summaryDao)
      lastTableFeature() shouldBe lastTable
      verify(analystDao, times(1)).groupByPuid(
        eq(sourceYtLastTable),
        eq(preparedYtTable),
        ?
      )
      verify(analystDao, times(2)).getDiff(
        eq(preparedYtTable),
        ?,
        ?
      )
      verify(summaryDao, times(1)).putBulkSummaryUpdates(fullBatchNonEmpty)
    }
  }

  private def runTask(analystDiffDao: AnalystDiffDao[F], summaryDao: SummaryDao[F]): Unit = {
    val task =
      new CalculateAndLoadDiffToYdbTask[F](
        config,
        analystDiffDao,
        summaryDao,
        lastTableFeature,
        batchSizeFeature
      )
    task.action.await
  }

  private def sourceYtTable(tableName: String): YtTable =
    YtTable.fromFolderAndName(config.sourceAnalystDataFolder, tableName)

  private def prepareYtTable(tableName: String): YtTable =
    YtTable.fromFolderAndName(config.preparedDataFolder, tableName)
}
