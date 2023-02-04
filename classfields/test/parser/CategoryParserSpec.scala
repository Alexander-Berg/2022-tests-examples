package ru.yandex.vertis.general.wizard.meta.parser

import general.bonsai.category_model.Category
import general.bonsai.export_model.ExportedEntity
import general.wizard.synonyms.CatalogSynonymsMapping
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.meta.resources.IntentionPragmaticsSnapshot
import ru.yandex.vertis.general.wizard.meta.service.impl.LiveDictionaryService
import ru.yandex.vertis.general.wizard.meta.utils.TestUtils
import ru.yandex.vertis.general.wizard.model.{MetaWizardRequest, RequestMatch}
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, testM, DefaultRunnableSpec, ZSpec}

object CategoryParserSpec extends DefaultRunnableSpec {

  private val bublik = Category(id = "bublic", synonyms = Seq("бублики"))
  private val sushka = Category(id = "sushka", synonyms = Seq("сушки"))
  private val tulskieBubliki = Category(id = "tulabublik", synonyms = Seq("тульские бублики"))

  private val snapshotSource: Seq[ExportedEntity] = Seq(
    ExportedEntity(ExportedEntity.CatalogEntity.Category(bublik)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(sushka)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(tulskieBubliki))
  )
  private val bonsaiSnapshot = BonsaiSnapshot(snapshotSource)
  private val intentions = Seq.empty

  private val intentionSnapshot = IntentionPragmaticsSnapshot(intentions)

  private val catalogSynonymsMapping = CatalogSynonymsMapping.defaultInstance

  private val dictionaryService =
    new LiveDictionaryService(
      intentionSnapshot,
      TestUtils.EmptyMetaPragmaticsSnapshot,
      bonsaiSnapshot,
      catalogSynonymsMapping
    )

  private val categoryParser = new CategoryParser(dictionaryService)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CategoryParser") {
      testM("parse category") {
        for {
          parsed <- categoryParser.parse(
            PartialParsedQuery(MetaWizardRequest.empty("купить сушки или бублики"), Set.empty)
          )
        } yield assert(parsed.toSet)(
          equalTo(
            Set(
              RequestMatch.Category.userInputIndices(Set(1), "sushka", 0L),
              RequestMatch.Category.userInputIndices(Set(3), "bublic", 0L)
            )
          )
        )
      }
    }

}
