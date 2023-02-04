package ru.yandex.vertis.general.bonsai.logic.test.public

import java.io.ByteArrayOutputStream

import general.bonsai.attribute_model.AttributeDefinition
import general.bonsai.category_model.Category
import general.bonsai.export_model.ExportedEntity
import ru.yandex.vertis.general.bonsai.logic.DefaultExportManager
import ru.yandex.vertis.general.bonsai.logic.public.InnerBonsaiSnapshot
import zio.stream.ZStream
import zio.{ZIO, ZManaged}

object PublicLogicTestUtils {

  def createSnapshotFromEntities(categories: Seq[Category], attributes: Seq[AttributeDefinition]): InnerBonsaiSnapshot =
    InnerBonsaiSnapshot.createSnapshot(
      categories.map(categoryToExport) ++
        attributes.map(attributeToExport)
    )

  def writeDelimitedToStream(
      categories: Seq[Category],
      attributes: Seq[AttributeDefinition]): ZIO[Any, Throwable, ZStream[Any, Nothing, Byte]] =
    ZManaged
      .makeEffect(new ByteArrayOutputStream())(_.close())
      .use { os =>
        ZIO
          .foreach(categories.map(categoryToExport) ++ attributes.map(attributeToExport)) { entity =>
            DefaultExportManager.serializeEntity(entity, os)
          }
          .as(os.toByteArray)
      }
      .map(ZStream.fromIterable(_))

  private def categoryToExport(category: Category) =
    ExportedEntity(ExportedEntity.CatalogEntity.Category(category))

  private def attributeToExport(attributeDefinition: AttributeDefinition) =
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(attributeDefinition))
}
