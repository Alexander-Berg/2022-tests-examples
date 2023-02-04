package ru.yandex.vertis.general.wizard.meta.resources

import general.bonsai.category_model.{Category, CategoryState}
import general.bonsai.attribute_model.AttributeDefinition
import general.bonsai.export_model.ExportedEntity
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, test, DefaultRunnableSpec, ZSpec}

object BonsaiSnapshotSpec extends DefaultRunnableSpec {

  private val instruments = Category(id = "instr", synonyms = Seq("инструмент"), state = CategoryState.DEFAULT)

  private val musicInstruments =
    Category(id = "music", synonyms = Seq("музыкальный инструмент"), state = CategoryState.DEFAULT)
  private val size = AttributeDefinition(id = "size", synonyms = Seq("размер"))

  private val snapshotSource: Seq[ExportedEntity] = Seq(
    ExportedEntity(ExportedEntity.CatalogEntity.Category(instruments)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(musicInstruments)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(size))
  )
  private val bonsaiSnapshot = BonsaiSnapshot(snapshotSource)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("BonsaiSnapshot")(
      test("load resource") {
        assert(bonsaiSnapshot.categories)(equalTo(Seq(instruments, musicInstruments))) &&
        assert(bonsaiSnapshot.attributes)(equalTo(Seq(size)))
      }
    )
}
