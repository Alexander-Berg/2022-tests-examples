package ru.yandex.vertis.general.bonsai.logic.test

import java.io.InputStream
import common.zio.clients.s3.S3Client
import common.zio.clients.s3.S3Client.S3Client
import common.zio.clients.s3.testkit.TestS3
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.bonsai.attribute_model.AttributeDefinition
import general.bonsai.category_model.{Category, CategoryAttribute, CategoryState}
import general.bonsai.export_model.ExportedEntity
import ru.yandex.vertis.general.bonsai.logic.ExportManager.ExportConfig
import ru.yandex.vertis.general.bonsai.logic.{AttributeManager, CategoryManager, ExportManager}
import ru.yandex.vertis.general.bonsai.model.CatalogVersionNotFound
import ru.yandex.vertis.general.bonsai.model.catalogValidation.{
  AttributeErrors,
  CatalogValidationReport,
  CategoryErrors
}
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
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.{ZIO, ZLayer}

object DefaultExportManagerTest extends DefaultRunnableSpec {

  private val MaxExportedVersions = 5

  private def exportConfig =
    ExportConfig("bucket", "export/prod", "export/master_", "export/report_", MaxExportedVersions)

  private def parseEntities(is: InputStream): Seq[ExportedEntity] = {
    ExportedEntity.parseDelimitedFrom(is) match {
      case None => Seq.empty[ExportedEntity]
      case Some(entity) => Seq(entity) ++ parseEntities(is)
    }
  }

  private def getUploaded(
      bucket: String,
      key: String): ZIO[S3Client with Blocking, S3Client.S3ClientException, Seq[ExportedEntity]] =
    S3Client.getObject(bucket, key).toInputStream.use { is =>
      ZIO.effectTotal(parseEntities(is))
    }

  private def isCategory: Assertion[ExportedEntity] = Assertion.assertion("isCategory")()(_.catalogEntity.isCategory)
  private def isAttribute: Assertion[ExportedEntity] = Assertion.assertion("isAttribute")()(_.catalogEntity.isAttribute)

  private def categoryUriPartEqualsTo(value: String): Assertion[ExportedEntity] =
    Assertion.assertion("categoryUriPartEqualsTo")()(_.getCategory.uriPart == value)

  private def attributeUriPartEqualsTo(value: String): Assertion[ExportedEntity] =
    Assertion.assertion("attributeUriPartEqualsTo")()(_.getAttribute.uriPart == value)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultExportManager")(
      testM("Not export same version twice") {
        for {
          version <- CategoryManager
            .createCategory(
              Category(uriPart = "same-version-1", uniqueName = "same-version-1-name", state = CategoryState.DEFAULT)
            )
            .map(_.version)
          _ <- ExportManager.exportCatalog
          _ <- ExportManager.exportCatalog
          versions <- ExportManager.getExportedCatalogVersions
          report <- ExportManager.getVersionReport(Some(version))
        } yield assert(versions)(equalTo(List(version))) &&
          assert(report)(equalTo(CatalogValidationReport.Success))
      },
      testM("Validate catalog upon export") {
        for {
          c1 <- CategoryManager.createCategory(
            Category(uriPart = "validate-1", uniqueName = "validate-name-1", state = CategoryState.FORBIDDEN)
          )
          a1 <- AttributeManager.createAttribute(
            AttributeDefinition(uriPart = "validate-3", uniqueName = "validate-name-3")
          )
          a2 <- AttributeManager
            .createAttribute(
              AttributeDefinition(uriPart = "validate-4", uniqueName = "validate-name-4")
            )
          c2 <- CategoryManager.createCategory(
            Category(
              uriPart = "validate-2",
              uniqueName = "validate-name-2",
              symlinkToCategoryId = c1.id,
              state = CategoryState.DEFAULT,
              attributes = Seq(CategoryAttribute(a1.id, a1.version), CategoryAttribute(a2.id, a2.version))
            )
          )
          c3 <- CategoryManager.createCategory(
            Category(
              uriPart = "non-terminal-1",
              uniqueName = "non-terminal-1-name",
              state = CategoryState.DEFAULT
            )
          )
          c4 <- CategoryManager.createCategory(
            Category(
              uriPart = "intermediate-1",
              uniqueName = "intermediate-1-name",
              state = CategoryState.DEFAULT,
              parentId = c3.id
            )
          )
          _ <- CategoryManager.createCategory(
            Category(
              uriPart = "leaf-1",
              uniqueName = "leaf-1-name",
              state = CategoryState.DEFAULT,
              parentId = c4.id
            )
          )
          a2u <- AttributeManager.updateAttribute(a2.copy(uniqueName = a1.uniqueName), a2.version)
          version = a2u.version
          _ <- ExportManager.exportCatalog
          report <- ExportManager.getVersionReport(Some(version))
          expected = CatalogValidationReport.Failed(
            attributeErrors = AttributeErrors(
              uniqueNameErrors = Seq(AttributeErrors.AttributeUniqueNameError(c2.id, a1.uniqueName, Seq(a1.id, a2.id))),
              Nil
            ),
            categoryErrors = CategoryErrors(Nil, Nil)
          )
        } yield assert(report)(equalTo(expected))
      },
      testM("Remove old versions") {
        for {
          allVersions <- ZIO.foreach(Iterator.from(1).take(MaxExportedVersions + 1).toList) { n =>
            for {
              version <- CategoryManager
                .createCategory(Category(uriPart = s"old-versions-$n", uniqueName = s"old-versions-$n-name"))
                .map(_.version)
              _ <- ExportManager.exportCatalog
            } yield version
          }
          exportedVersions <- ExportManager.getExportedCatalogVersions
        } yield assert(exportedVersions)(hasSameElements(allVersions.drop(1)))
      },
      testM("Remove all exported versions") {
        for {
          _ <- ZIO.foreach(Iterator.from(1).take(MaxExportedVersions).toList) { n =>
            for {
              version <- CategoryManager
                .createCategory(
                  Category(uriPart = s"remove-all-versions-$n", uniqueName = s"remove-all-versions-$n-name")
                )
                .map(_.version)
              _ <- ExportManager.exportCatalog
            } yield version
          }
          _ <- ExportManager.removeAllExportedCatalogs
          exportedVersions <- ExportManager.getExportedCatalogVersions
        } yield assert(exportedVersions)(isEmpty)
      },
      testM("Use latest exported catalog") {
        for {
          version <- CategoryManager
            .createCategory(Category(uriPart = "useLatestExport", uniqueName = "useLatestExport-name"))
            .map(_.version)
          _ <- ExportManager.exportCatalog
          _ <- ExportManager.useExportedCatalog(None)
          uploadedData <- getUploaded(exportConfig.s3Bucket, exportConfig.inUseCatalogFilename)
          versionInUse <- ExportManager.getInUseCatalogVersion
        } yield assert(uploadedData)(exists(isCategory && categoryUriPartEqualsTo("useLatestExport"))) &&
          assert(versionInUse)(equalTo(version))
      },
      testM("Use exported catalog of specific version") {
        for {
          categoryVersion <- CategoryManager
            .createCategory(Category(uriPart = "useSpecificExport", uniqueName = "useSpecificExport-name"))
            .map(_.version)
          _ <- ExportManager.exportCatalog
          attributeVersion <-
            AttributeManager
              .createAttribute(
                AttributeDefinition(uriPart = "useSpecificExport", uniqueName = "useSpecificExport-name")
              )
              .map(_.version)
          _ <- ExportManager.exportCatalog

          _ <- ExportManager.useExportedCatalog(Some(attributeVersion))
          attributeVersionInUse <- ExportManager.getInUseCatalogVersion
          uploadedDataAttributeVersion <- getUploaded(exportConfig.s3Bucket, exportConfig.inUseCatalogFilename)

          _ <- ExportManager.useExportedCatalog(Some(categoryVersion))
          categoryVersionInUse <- ExportManager.getInUseCatalogVersion
          uploadedDataCategoryVersion <- getUploaded(exportConfig.s3Bucket, exportConfig.inUseCatalogFilename)
        } yield assert(uploadedDataAttributeVersion)(
          exists(isAttribute && attributeUriPartEqualsTo("useSpecificExport")) &&
            exists(isCategory && categoryUriPartEqualsTo("useSpecificExport"))
        ) &&
          assert(uploadedDataCategoryVersion)(
            not(exists(isAttribute && attributeUriPartEqualsTo("useSpecificExport"))) &&
              exists(isCategory && categoryUriPartEqualsTo("useSpecificExport"))
          ) &&
          assert(attributeVersionInUse)(equalTo(attributeVersion)) &&
          assert(categoryVersionInUse)(equalTo(categoryVersion))
      },
      testM("Fail if exported version does not exist") {
        for {
          error <- ExportManager.useExportedCatalog(Some(100500L)).run
        } yield assert(error)(fails(equalTo(CatalogVersionNotFound("100500"))))
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
      val testS3 = TestS3.mocked >>> ZLayer.fromEffect {
        for {
          s3 <- ZIO.service[S3Client.Service]
          _ <- s3.createBucket(exportConfig.s3Bucket).orDie
        } yield s3
      }
      val export = ZLayer.succeed(exportConfig)
      val deps =
        entityDao ++ historyDao ++ globalHistoryDao ++ constraintsDao ++ suggestIndexDao ++ hierarchyDao ++ txRunner ++ clock ++
          blocking ++ testS3 ++ export ++ logging
      (deps >>> CategoryManager.live) ++ (deps >>> AttributeManager.live) ++ (deps >>> ExportManager.live) ++ testS3
    }
  }
}
