package ru.yandex.vertis.general.bonsai.logic.test

import java.nio.charset.StandardCharsets
import common.zio.clients.s3.S3Client
import common.zio.clients.s3.testkit.TestS3
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.bonsai.logic.{CategoryManager, ExportManager, StructureExportManager}
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.TestAspect._
import zio.test._
import zio.test.Assertion._
import general.bonsai.category_model.{Category, CategoryState}
import ru.yandex.vertis.general.bonsai.logic.ExportManager.ExportConfig
import ru.yandex.vertis.general.bonsai.storage.ydb.sign.EntityUpdateChecker
import ru.yandex.vertis.general.bonsai.storage.ydb.{
  BonsaiTxRunner,
  YdbConstraintsDao,
  YdbEntityDao,
  YdbGlobalHistoryDao,
  YdbHierarchyDao,
  YdbHistoryDao,
  YdbSuggestEntityIndexDao
}
import common.zio.logging.Logging
import zio.{ZIO, ZLayer}
import zio.blocking.Blocking
import zio.clock.Clock

import scala.io.Source.fromResource

object DefaultStructureExportManagerTest extends DefaultRunnableSpec {

  private def exportConfig =
    ExportConfig("bucket", "export/prod", "export/master_", "export/report_", 1)

  private def exportStructConfig = StructureExportManager.Config("bucket", "catalog_structure", "bucket", "export/prod")

  private def createCategory(
      name: String,
      uniqueName: String,
      state: CategoryState = CategoryState.DEFAULT,
      parent: String = "",
      symlinkTo: String = "",
      ignoreCondition: Boolean = false) = {
    CategoryManager.createCategory(
      Category(
        uriPart = uniqueName.toLowerCase.replaceAll("\\s", "_"),
        uniqueName = uniqueName,
        name = name,
        parentId = parent,
        symlinkToCategoryId = symlinkTo,
        state = state,
        ignoreCondition = ignoreCondition
      )
    )
  }

  private def readStructure = for {
    config <- ZIO.service[StructureExportManager.Config]
    s3 <- ZIO.service[S3Client.Service]
    csv <- s3
      .getObject(config.bucket, s"${config.fileName}.csv")
      .runCollect
      .map(chunk => new String(chunk.toArray, StandardCharsets.UTF_8))
  } yield csv

  private def readExpected(fileName: String) = ZIO {
    fromResource(fileName)(scala.io.Codec.UTF8).mkString
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultStructureExportManager")(
      testM("Build correct structure file") {
        for {
          e <- createCategory("Электроника", "электроника")
          d <- createCategory("Драм-н-бейс", "днб", parent = e.id)
          nf <- createCategory("Нейрофанк", "нейрофанк", parent = d.id, ignoreCondition = true)
          _ <- createCategory("Брейккор", "брейккор", parent = d.id, state = CategoryState.FORBIDDEN)
          _ <- createCategory("Чиптюн", "чиптюн", state = CategoryState.ARCHIVED)
          _ <- createCategory("Super Eurobeat", "евробит", state = CategoryState.FORBIDDEN)
          _ <- createCategory("Нейрофанк с гуслями", "нейрофанк - 2", parent = d.id, symlinkTo = nf.id)
          _ <- ExportManager.exportCatalog
          _ <- ExportManager.useExportedCatalog(None)
          _ <- StructureExportManager.exportCatalogStructure
          structure <- readStructure
          expected <- readExpected("example_1.csv")
        } yield assert(structure)(equalTo(expected))
      }
    ).provideCustomLayer {
      val clock = Clock.live
      val blocking = Blocking.live
      val logging = Logging.live
      val ydb = TestYdb.ydb
      val updateChecker = EntityUpdateChecker.live
      val txRunner = ((ydb >>> Ydb.txRunner) ++ updateChecker) >>> BonsaiTxRunner.live
      val entityDao = (ydb ++ updateChecker) >>> YdbEntityDao.live
      val historyDao = ydb >>> YdbHistoryDao.live
      val globalHistoryDao = ydb >>> YdbGlobalHistoryDao.live
      val constraintsDao = ydb >>> YdbConstraintsDao.live
      val hierarchyDao = ydb >>> YdbHierarchyDao.live
      val suggestIndexDao = ydb >>> YdbSuggestEntityIndexDao.live
      val testS3 = TestS3.mocked
      val exportConfigLayer = ZLayer.succeed(exportConfig)
      val exportStructConfigLayer = ZLayer.succeed(exportStructConfig)
      val deps =
        entityDao ++ historyDao ++ globalHistoryDao ++ constraintsDao ++ suggestIndexDao ++ hierarchyDao ++ txRunner ++ clock ++ logging ++
          blocking ++ testS3 ++ exportConfigLayer ++ exportStructConfigLayer
      (deps >>> CategoryManager.live) ++ (deps >>> ExportManager.live) ++ (deps >>> StructureExportManager.Live) ++ testS3 ++ exportStructConfigLayer
    }
  }
}
