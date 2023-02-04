package ru.yandex.vertis.general.wizard.meta.services

import general.bonsai.category_model.Category
import general.bonsai.export_model.ExportedEntity
import general.wizard.synonyms.CatalogSynonymsMapping
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.meta.resources.IntentionPragmaticsSnapshot
import ru.yandex.vertis.general.wizard.meta.service.impl.LiveDictionaryService
import ru.yandex.vertis.general.wizard.meta.utils.TestUtils
import ru.yandex.vertis.general.wizard.model.{IntentionType, Pragmatic}
import zio.test.Assertion.equalTo
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object DictionaryServiceSpec extends DefaultRunnableSpec {

  private val instruments = Category(id = "instr", synonyms = Seq("Инструмент"))
  private val musicInstruments = Category(id = "music", synonyms = Seq(" музыкальный    инсТрумент "))
  private val brand = Pragmatic.Intention(IntentionType.Brand, "brand", Set("ЯНдекс"))
  private val newIntention = Pragmatic.Intention(IntentionType.New, "new", Set("  ноВый"))

  private val snapshotSource: Seq[ExportedEntity] = Seq(
    ExportedEntity(ExportedEntity.CatalogEntity.Category(instruments)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(musicInstruments))
  )
  private val bonsaiSnapshot = BonsaiSnapshot(snapshotSource)
  private val intentions = Seq(brand, newIntention)

  private val intentionSnapshot = IntentionPragmaticsSnapshot(intentions)

  private val catalogSynonymsMapping = CatalogSynonymsMapping.defaultInstance

  private val dictionaryService = new LiveDictionaryService(
    intentionSnapshot,
    TestUtils.EmptyMetaPragmaticsSnapshot,
    bonsaiSnapshot,
    catalogSynonymsMapping
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DictionaryService")(
      testM("normalize category aliases") {
        for {
          dict <- dictionaryService.categoryTokensDictionary
        } yield assert(dict.keys.toSet)(
          equalTo(
            Set("инструмент", "музыкальный")
          )
        )
      },
      testM("normalize intention aliases") {
        for {
          dict <- dictionaryService.intentionTokensDictionary
        } yield assert(dict.keys.toSet)(
          equalTo(
            Set("яндекс", "новый")
          )
        )
      }
    )
}
